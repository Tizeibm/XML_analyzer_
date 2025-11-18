package com.xml.models;

public class NavigationResponse {
        public boolean success;
        public String message;
        public XMLError error;
        public String preciseRange;
        public boolean hasZone;
        public String zoneContent;
        public int zoneStartLine;
        public int zoneEndLine;

        public NavigationResponse() {}

        public NavigationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }