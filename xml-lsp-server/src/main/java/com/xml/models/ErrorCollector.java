package com.xml.models;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumule les erreurs détectées pendant le parsing SAX et la validation XSD.
 * Thread-safe pour support multi-threading.
 */
public class ErrorCollector {


    private final List<XMLError> errors = Collections.synchronizedList(new ArrayList<>());

    public void addError(String message, int lineNumber, String type) {
        XMLError err = new XMLError(message, lineNumber, type);
        errors.add(err);

    }

    public List<XMLError> getErrors() {
        return List.copyOf(errors); // snapshot immuable
    }

    public void clear() {
        errors.clear();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int getErrorCount() {
        return errors.size();
    }
}