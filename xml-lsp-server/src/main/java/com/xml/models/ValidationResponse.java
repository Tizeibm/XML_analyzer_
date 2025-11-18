package com.xml.models;

import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

public  class ValidationResponse {
        public boolean success;
        public String message;
        public List<Diagnostic> diagnostics;
        public List<XMLError> errors;
        public long fileSize;
        public long validationTime;
        public int errorCount;
        public int warningCount;
        public String summary;

        public ValidationResponse() {}

        public ValidationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.diagnostics = new ArrayList<>();
            this.errors = new ArrayList<>();
        }
    }