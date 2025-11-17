package com.xml.models;

/**
 * Paramètres pour la notification xml/structureError
 */
public class StructureErrorParams {
    public String xmlUri;
    public String message;
    public int lineNumber;
    public int columnNumber;
    public String tagName;
    public String errorType;
    
    public StructureErrorParams(String xmlUri, String message, int lineNumber, 
                               int columnNumber, String tagName, String errorType) {
        this.xmlUri = xmlUri;
        this.message = message;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.tagName = tagName;
        this.errorType = errorType;
    }
}