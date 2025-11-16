package com.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumule les erreurs détectées pendant le parsing SAX et la validation XSD.
 * Thread-safe pour support multi-threading.
 */
public class ErrorCollector {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorCollector.class);
    private final List<XMLError> errors = Collections.synchronizedList(new ArrayList<>());

    public void addError(String message, int lineNumber, String type) {
        XMLError err = new XMLError(message, lineNumber, type);
        errors.add(err);
        LOG.error(err.toString());
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
