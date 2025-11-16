package com.xml;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Paramètres pour la notification xml/validationResults
 */
public class ValidationResultsParams {
    public String xmlUri;
    public JsonArray errors;
    public int errorCount;
    public int warningCount;
    
    public ValidationResultsParams(String xmlUri, JsonArray errors, int errorCount, int warningCount) {
        this.xmlUri = xmlUri;
        this.errors = errors;
        this.errorCount = errorCount;
        this.warningCount = warningCount;
    }
}