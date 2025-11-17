package com.xml;

import com.xml.handlers.TrackedStaxHandler;
import com.xml.models.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Analyse SAX en streaming (adapté aux très gros fichiers).
 * Les erreurs sont collectées sans interrompre le parsing (tolérance).
 * Aucun fichier n'est chargé entièrement en mémoire.
 */
public class XMLParser {

    private static final Logger LOG = LoggerFactory.getLogger(XMLParser.class);
    private final ErrorCollector errorCollector;

    public XMLParser(ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
    }

    /**
     * Parsing STREAMING d'un fichier XML (peut peser des Go).
     * Le fichier n'est JAMAIS chargé entièrement en mémoire.
     */
    public void parse(File xmlFile) {
        LOG.info("Début parsing StAX : {}", xmlFile.getAbsolutePath());
        try (InputStream in = new FileInputStream(xmlFile)) {
            new TrackedStaxHandler(errorCollector).parse(in);   // ← unique ligne changée
            LOG.info("Parsing StAX terminé. Erreurs : {}", errorCollector.getErrorCount());
        } catch (IOException e) {
            errorCollector.addError("Fichier illisible : " + e.getMessage(), 0, "FATAL_PARSE");
            LOG.error("Erreur entrée/sortie", e);
        }
    }
}