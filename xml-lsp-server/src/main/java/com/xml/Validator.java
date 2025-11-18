package com.xml;

import com.xml.models.ErrorCollector;


import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Valide un fichier XML contre un schéma XSD en streaming.
 * Les erreurs de validation sont stockées dans l'com.xml.ErrorCollector fourni.
 */
public class Validator {


    private final ErrorCollector collector;

    public Validator(ErrorCollector collector) {
        this.collector = collector;
    }

    public boolean validate(File xmlFile, File xsdFile) {
        if (xsdFile == null || !xsdFile.exists()) {

            return true;
        }

        

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            //factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
           // factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Schema schema = factory.newSchema(xsdFile);
            javax.xml.validation.Validator validator = schema.newValidator();
            validator.setErrorHandler(new XsdErrorHandler(collector));

            try (var is = Files.newInputStream(xmlFile.toPath())) {
                validator.validate(new StreamSource(is));
            }
            
            return true;
        } catch (SAXException | IOException e) {
            collector.addError("Validation XSD échouée : " + e.getMessage(), 0, "FATAL_VALIDATION");

            return false;
        }
    }

    private static class XsdErrorHandler extends DefaultHandler {
        private final ErrorCollector col;
        
        XsdErrorHandler(ErrorCollector c) { 
            col = c; 
        }
        
        @Override 
        public void warning(SAXParseException e) { 
            col.addError(e.getMessage(), e.getLineNumber(), "VALIDATION_WARNING"); 
        }
        
        @Override 
        public void error(SAXParseException e) { 
            col.addError(e.getMessage(), e.getLineNumber(), "VALIDATION_ERROR"); 
        }
        
        @Override 
        public void fatalError(SAXParseException e) throws SAXException {
            col.addError(e.getMessage(), e.getLineNumber(), "FATAL_VALIDATION");
            throw e;
        }
    }
}