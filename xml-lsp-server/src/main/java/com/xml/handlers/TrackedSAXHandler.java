package com.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.Stack;

/**
 * Suit les balises ouvertes et détecte :
 *  - balise fermante inattendue
 *  - balise non fermée en fin de document
 *  - erreurs SAX classiques (syntaxe)
 * Tout est stocké dans com.xml.ErrorCollector pour être renvoyé au client LSP.
 */
public class TrackedSAXHandler extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TrackedSAXHandler.class);
    private final ErrorCollector collector;
    private Locator locator; // fourni par le parseur
    private final Stack<TagInfo> stack = new Stack<>();

    private static class TagInfo {
        final String qName;
        final int line;
        TagInfo(String q, int l) { qName = q; line = l; }
    }

    public TrackedSAXHandler(ErrorCollector collector) {
        this.collector = collector;
    }

    @Override
    public void setDocumentLocator(Locator loc) {
        this.locator = loc;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        stack.push(new TagInfo(qName, locator.getLineNumber()));
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (stack.isEmpty()) {
            collector.addError("Balise fermante inattendue : </" + qName + ">", locator.getLineNumber(), "STRUCTURE");
        }
        TagInfo attendu = stack.pop();
        if (!attendu.qName.equals(qName)) {
            collector.addError("Attendu </" + attendu.qName + "> mais trouvé </" + qName + ">", locator.getLineNumber(), "STRUCTURE");
            stack.push(attendu);
        }

    }

    @Override
    public void endDocument() {
        // Toutes les balises encore dans la pile n'ont pas été refermées
        while (!stack.isEmpty()) {
            TagInfo tag = stack.pop();
            collector.addError("Balise fermante manquante pour <" + tag.qName + ">", tag.line, "STRUCTURE");
        }
    }

    @Override
    public void warning(SAXParseException e) {
        collector.addError(e.getMessage(), e.getLineNumber(), "WARNING");
    }

    @Override
    public void error(SAXParseException e) {
        collector.addError(e.getMessage(), e.getLineNumber(), "SYNTAX");
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        collector.addError(e.getMessage(), e.getLineNumber(), "FATAL_SYNTAX");
        throw e;
    }
}
