package com.xml.models;

import java.util.List;

public class ZoneExtractionResponse {
        public boolean success;
        public List<XMLError> errors;
        public String message;

        public ZoneExtractionResponse(boolean success, List<XMLError> errors, String message) {
            this.success = success;
            this.errors = errors;
            this.message = message;
        }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<XMLError> getErrors() {
        return errors;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setErrors(List<XMLError> errors) {
        this.errors = errors;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}