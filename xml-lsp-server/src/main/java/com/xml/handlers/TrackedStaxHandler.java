package com.xml.handlers;

import com.xml.models.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * StAX-based handler pour détecter les balises non fermées avec précision.
 * Rapporte l'erreur à la ligne/colonne exacte de la balise ouvrante.
 */
public class TrackedStaxHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TrackedStaxHandler.class);
    private final ErrorCollector collector;

    // Stack pour suivre les balises ouvertes avec leur position exacte
    private final Deque<TagInfo> tagStack = new ArrayDeque<>();

    private static class TagInfo {
        final String name;
        final int lineNumber;
        final int columnNumber;

        TagInfo(String name, int lineNumber, int columnNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
    }

    public TrackedStaxHandler(ErrorCollector collector) {
        this.collector = collector;
    }

    public void parse(InputStream in) {
        XMLInputFactory factory = XMLInputFactory.newFactory();

        // Configuration de sécurité
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        XMLEventReader reader = null;

        try {
            reader = factory.createXMLEventReader(in);

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartElement()) {
                    handleStartElement(event.asStartElement());
                } else if (event.isEndElement()) {
                    handleEndElement(event.asEndElement());
                }
                // Note: Les balises auto-fermantes (<tag/>) sont gérées automatiquement
                // car elles génèrent un StartElement immédiatement suivi d'un EndElement
            }

            // À la fin du parsing, vérifier les balises non fermées
            checkUnclosedTags();

        } catch (XMLStreamException ex) {
            handleParseError(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOG.debug("Erreur lors de la fermeture du reader", e);
                }
            }
        }
    }

    private void handleStartElement(javax.xml.stream.events.StartElement startElement) {
        String tagName = startElement.getName().getLocalPart();
        Location location = startElement.getLocation();

        int line = location.getLineNumber();
        int column = location.getColumnNumber();

        //LOG.debug("Balise ouvrante: <{}> à ligne {}, colonne {}", tagName, line, column);

        // Empiler la balise avec sa position
        tagStack.push(new TagInfo(tagName, line, column));
    }

    private void handleEndElement(javax.xml.stream.events.EndElement endElement) {
        String tagName = endElement.getName().getLocalPart();
        Location location = endElement.getLocation();

        //LOG.debug("Balise fermante: </{}> à ligne {}", tagName, location.getLineNumber());

        if (tagStack.isEmpty()) {
            // Balise fermante sans balise ouvrante correspondante
            collector.addError(
                    "Balise fermante </" + tagName + "> sans balise ouvrante correspondante",
                    location.getLineNumber(),
                    "STRUCTURE"
            );
            return;
        }

        TagInfo lastOpenedTag = tagStack.peek();

        if (!lastOpenedTag.name.equals(tagName)) {
            // Mismatch de balises - cela indique souvent une fermeture manquante
            collector.addError(
                    "Balise fermante </" + tagName + "> inattendue. Attendu </" + lastOpenedTag.name + ">",
                    location.getLineNumber(),
                    "STRUCTURE"
            );

            // On peut essayer de retrouver la balise correspondante dans la stack
            recoverFromMismatch(tagName, location.getLineNumber());
        } else {
            // Balises correspondantes - dépiler normalement
            tagStack.pop();
        }
    }

    private void recoverFromMismatch(String foundTagName, int errorLine) {
        // Rechercher dans la stack si cette balise a été ouverte précédemment
        Deque<TagInfo> tempStack = new ArrayDeque<>();
        boolean found = false;

        while (!tagStack.isEmpty()) {
            TagInfo tag = tagStack.pop();
            tempStack.push(tag);

            if (tag.name.equals(foundTagName)) {
                found = true;
                // Signaler la balise ouvrante non fermée
                collector.addError(
                        "Balise <" + foundTagName + "> non fermée (ouverte à ligne " + tag.lineNumber + ")",
                        tag.lineNumber,
                        "STRUCTURE"
                );
                break;
            }
        }

        // Remettre les autres balises dans la stack
        while (!tempStack.isEmpty()) {
            TagInfo tag = tempStack.pop();
            if (!tag.name.equals(foundTagName) || !found) {
                tagStack.push(tag);
            }
        }

        if (!found) {
            // Balise fermante orpheline
            collector.addError(
                    "Balise fermante </" + foundTagName + "> sans ouvrante correspondante",
                    errorLine,
                    "STRUCTURE"
            );
        }
    }

    private void checkUnclosedTags() {
        // Après le parsing complet, toutes les balises encore dans la stack sont non fermées
        while (!tagStack.isEmpty()) {
            TagInfo unclosedTag = tagStack.pop();

            String message = String.format(
                    "Balise <%s> non fermée (ouverte à ligne %d, colonne %d)",
                    unclosedTag.name,
                    unclosedTag.lineNumber,
                    unclosedTag.columnNumber
            );

            collector.addError(
                    message,
                    unclosedTag.lineNumber,  // Ligne de la balise OUVERTE
                    "STRUCTURE"
            );

            LOG.warn("Balise non fermée détectée: {} à ligne {}",
                    unclosedTag.name, unclosedTag.lineNumber);
        }
    }

    private void handleParseError(XMLStreamException ex) {
        Location location = ex.getLocation();
        int line = (location != null) ? location.getLineNumber() : 0;
        int column = (location != null) ? location.getColumnNumber() : 0;

        String message = ex.getMessage();
        String type = "SYNTAX";

        // Analyser le type d'erreur pour un message plus précis
        if (message != null) {
            if (message.contains("must be terminated by the matching end-tag")) {
                type = "STRUCTURE";
                // Extraire le nom de la balise concernée si possible
                if (message.contains("element type")) {
                    String tagName = extractTagNameFromErrorMessage(message);
                    if (tagName != null && !tagStack.isEmpty()) {
                        // Chercher la dernière balise ouverte correspondante
                        for (TagInfo tag : tagStack) {
                            if (tag.name.equals(tagName)) {
                                message = "Balise <" + tagName + "> non fermée (ouverte à ligne " +
                                        tag.lineNumber + ", colonne " + tag.columnNumber + ")";
                                line = tag.lineNumber;
                                break;
                            }
                        }
                    }
                }
            } else if (message.contains("DOCTYPE")) {
                type = "WARNING";
            } else if (message.contains("namespace") || message.contains("Prefix")) {
                type = "NAMESPACE";
            }
        }

        collector.addError(message, line, type);
        LOG.debug("Erreur de parsing à ligne {}: {}", line, message);
    }

    private String extractTagNameFromErrorMessage(String message) {
        // Tente d'extraire le nom de balise d'un message d'erreur comme:
        // "The element type "book" must be terminated by the matching end-tag "</book>"."
        try {
            int start = message.indexOf('"');
            if (start != -1) {
                int end = message.indexOf('"', start + 1);
                if (end != -1) {
                    return message.substring(start + 1, end);
                }
            }
        } catch (Exception e) {
            LOG.debug("Impossible d'extraire le nom de balise du message d'erreur", e);
        }
        return null;
    }
}
