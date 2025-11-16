package com.xml;

/**
 * Représente une erreur XML détectée (syntaxe, structure, validation).
 */
public class XMLError {
    public final String message;
    public final int    lineNumber;
    public final String type;   // SYNTAX, STRUCTURE, VALIDATION_ERROR, WARNING, FATAL_*

    private String id;
    private int column;
    private String severity; // "error", "warning"
    private String code;
    private String fragment;
    private int fragmentStartLine;
    private int fragmentEndLine;
    private String tagName;

    private int preciseStartLine;
    private int preciseStartColumn;
    private int preciseEndLine;
    private int preciseEndColumn;

    private String zoneContent;
    private int zoneStartLine;
    private int zoneEndLine;
    private boolean zoneExtracted = false;

    public XMLError(String message, int lineNumber, String type) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.type = type;
        this.id = generateId();
        this.column = 1;
        this.severity = mapSeverity(type);
        this.code = type;

        this.preciseStartLine = lineNumber;
        this.preciseStartColumn = 1;
        this.preciseEndLine = lineNumber;
        this.preciseEndColumn = 30;
    }

    public String getMessage() {
        return message;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getType() {
        return type;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getId() { return id; }
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }
    
    public String getSeverity() { return severity; }
    public String getCode() { return code; }
    
    public String getFragment() { return fragment; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    
    public int getFragmentStartLine() { return fragmentStartLine; }
    public void setFragmentStartLine(int line) { this.fragmentStartLine = line; }
    
    public int getFragmentEndLine() { return fragmentEndLine; }
    public void setFragmentEndLine(int line) { this.fragmentEndLine = line; }

    @Override
    public String toString() {
        return "[" + type + "] ligne " + lineNumber + " : " + message;
    }

    private String generateId() {
        return "error-" + System.nanoTime();
    }

    private String mapSeverity(String type) {
        if (type.contains("FATAL")) return "error";
        if (type.equals("WARNING")) return "warning";
        return "error";
    }

    public int getPreciseStartLine() { return preciseStartLine; }
    public void setPreciseStartLine(int line) { this.preciseStartLine = line; }

    public int getPreciseStartColumn() { return preciseStartColumn; }
    public void setPreciseStartColumn(int column) { this.preciseStartColumn = column; }

    public int getPreciseEndLine() { return preciseEndLine; }
    public void setPreciseEndLine(int line) { this.preciseEndLine = line; }

    public int getPreciseEndColumn() { return preciseEndColumn; }
    public void setPreciseEndColumn(int column) { this.preciseEndColumn = column; }

    // Méthode utilitaire pour définir la position précise complète
    public void setPrecisePosition(int startLine, int startColumn, int endLine, int endColumn) {
        this.preciseStartLine = startLine;
        this.preciseStartColumn = startColumn;
        this.preciseEndLine = endLine;
        this.preciseEndColumn = endColumn;
    }

    // Méthode pour obtenir la position précise au format JSON
    public String getPreciseRangeJson() {
        return String.format(
                "{\"startLine\":%d,\"startColumn\":%d,\"endLine\":%d,\"endColumn\":%d}",
                preciseStartLine, preciseStartColumn, preciseEndLine, preciseEndColumn
        );
    }

    public String getZoneContent() { return zoneContent; }
    public void setZoneContent(String zoneContent) {
        this.zoneContent = zoneContent;
        this.zoneExtracted = true;
    }

    public int getZoneStartLine() { return zoneStartLine; }
    public void setZoneStartLine(int zoneStartLine) { this.zoneStartLine = zoneStartLine; }

    public int getZoneEndLine() { return zoneEndLine; }
    public void setZoneEndLine(int zoneEndLine) { this.zoneEndLine = zoneEndLine; }

    public boolean isZoneExtracted() { return zoneExtracted; }
    public void markZoneExtracted() { this.zoneExtracted = true; }

    /**
     * Définit la zone complète
     */
    public void setZone(String content, int startLine, int endLine) {
        this.zoneContent = content;
        this.zoneStartLine = startLine;
        this.zoneEndLine = endLine;
        this.zoneExtracted = true;
    }

    /**
     * Obtient la zone formatée pour l'affichage
     */
    public String getFormattedZone() {
        if (!zoneExtracted || zoneContent == null) {
            return "<!-- Zone non extraite -->";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!-- Zone d'erreur (lignes ")
                .append(zoneStartLine).append("-").append(zoneEndLine).append(") -->\n")
                .append(zoneContent)
                .append("\n<!-- Fin zone d'erreur -->");

        return sb.toString();
    }
}
