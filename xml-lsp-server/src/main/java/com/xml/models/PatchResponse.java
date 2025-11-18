package com.xml.models;

public  class PatchResponse {
        public boolean success;
        public String message;

        public PatchResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }