package com.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrait des zones spécifiques de gros fichiers XML sans tout charger en mémoire.
 * Optimisé pour les fichiers de plusieurs To.
 */
public class XmlZoneExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(XmlZoneExtractor.class);
    
    // Taille du buffer pour la lecture optimisée
    private static final int BUFFER_SIZE = 8192;
    private static final int ZONE_CONTEXT_LINES = 3; // Lignes de contexte autour de l'erreur

    /**
     * Extrait une zone précise autour d'une ligne d'erreur
     * @param xmlFile Fichier XML
     * @param errorLine Ligne de l'erreur (1-based)
     * @return Fragment XML autour de l'erreur
     */
    public static XmlZone extractErrorZone(File xmlFile, int errorLine) {
        return extractZone(xmlFile, errorLine, ZONE_CONTEXT_LINES);
    }

    /**
     * Extrait une zone spécifique avec contexte
     */
    public static XmlZone extractZone(File xmlFile, int centerLine, int contextLines) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (!xmlFile.exists() || !xmlFile.canRead()) {
                throw new IllegalArgumentException("Fichier inaccessible: " + xmlFile.getAbsolutePath());
            }

            int startLine = Math.max(1, centerLine - contextLines);
            int endLine = centerLine + contextLines;
            
            List<String> zoneLines = new ArrayList<>();
            int currentLine = 0;
            boolean zoneStarted = false;

            // Lecture optimisée ligne par ligne
            try (BufferedReader reader = new BufferedReader(new FileReader(xmlFile), BUFFER_SIZE)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    currentLine++;
                    
                    if (currentLine >= startLine && currentLine <= endLine) {
                        zoneLines.add(line);
                        zoneStarted = true;
                    } else if (currentLine > endLine) {
                        break; // On s'arrête une fois la zone extraite
                    }
                }
            }

            long extractionTime = System.currentTimeMillis() - startTime;
            LOG.debug("Zone extraite en {}ms : lignes {}-{}", extractionTime, startLine, endLine);
            
            return new XmlZone(
                String.join("\n", zoneLines),
                startLine,
                endLine,
                centerLine,
                zoneLines.size()
            );
            
        } catch (Exception e) {
            LOG.error("Erreur extraction zone ligne {}: {}", centerLine, e.getMessage());
            return XmlZone.EMPTY_ZONE;
        }
    }

    /**
     * Extrait une zone par position approximative (pour très gros fichiers)
     */
    public static XmlZone extractZoneByApproximatePosition(File xmlFile, long approximatePosition, int contextSize) {
        try {
            long fileSize = xmlFile.length();
            long startPos = Math.max(0, approximatePosition - contextSize);
            long endPos = Math.min(fileSize, approximatePosition + contextSize);
            
            byte[] buffer = new byte[(int)(endPos - startPos)];
            
            try (RandomAccessFile raf = new RandomAccessFile(xmlFile, "r")) {
                raf.seek(startPos);
                raf.read(buffer);
                
                String content = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
                return new XmlZone(content, 0, 0, 0, countLines(content));
            }
            
        } catch (Exception e) {
            LOG.error("Erreur extraction zone position {}: {}", approximatePosition, e.getMessage());
            return XmlZone.EMPTY_ZONE;
        }
    }

    /**
     * Trouve la balise parente pour mieux contextualiser l'erreur
     */
    public static XmlZone extractZoneWithParentContext(File xmlFile, int errorLine, String tagName) {
        try {
            List<String> contextLines = new ArrayList<>();
            int startLine = Math.max(1, errorLine - 10); // Plus large contexte
            int endLine = errorLine + 5;
            int currentLine = 0;
            String parentTag = null;

            try (BufferedReader reader = new BufferedReader(new FileReader(xmlFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    currentLine++;
                    
                    if (currentLine >= startLine && currentLine <= endLine) {
                        contextLines.add(line);
                        
                        // Détecter la balise parente pour mieux contextualiser
                        if (parentTag == null && line.contains("<") && !line.contains("</")) {
                            String tag = extractOpeningTag(line);
                            if (tag != null && !tag.equals(tagName)) {
                                parentTag = tag;
                            }
                        }
                    } else if (currentLine > endLine) {
                        break;
                    }
                }
            }

            String zoneContent = String.join("\n", contextLines);
            return new XmlZone(zoneContent, startLine, endLine, errorLine, contextLines.size(), parentTag);
            
        } catch (Exception e) {
            LOG.error("Erreur extraction zone avec contexte: {}", e.getMessage());
            return XmlZone.EMPTY_ZONE;
        }
    }

    private static String extractOpeningTag(String line) {
        // Extraire le nom de balise d'une ligne
        int start = line.indexOf('<');
        if (start == -1) return null;
        
        int end = line.indexOf('>', start);
        if (end == -1) return null;
        
        int spaceIndex = line.indexOf(' ', start);
        if (spaceIndex != -1 && spaceIndex < end) {
            end = spaceIndex;
        }
        
        return line.substring(start + 1, end).trim();
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        return text.split("\r\n|\r|\n").length;
    }
    
    /**
     * Représente une zone extraite d'un fichier XML
     */
    public static class XmlZone {
        public static final XmlZone EMPTY_ZONE = new XmlZone("", 0, 0, 0, 0);
        
        private final String content;
        private final int startLine;
        private final int endLine;
        private final int centerLine;
        private final int lineCount;
        private final String parentTag;
        
        public XmlZone(String content, int startLine, int endLine, int centerLine, int lineCount) {
            this(content, startLine, endLine, centerLine, lineCount, null);
        }
        
        public XmlZone(String content, int startLine, int endLine, int centerLine, int lineCount, String parentTag) {
            this.content = content;
            this.startLine = startLine;
            this.endLine = endLine;
            this.centerLine = centerLine;
            this.lineCount = lineCount;
            this.parentTag = parentTag;
        }
        
        // Getters
        public String getContent() { return content; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public int getCenterLine() { return centerLine; }
        public int getLineCount() { return lineCount; }
        public String getParentTag() { return parentTag; }
        public boolean isEmpty() { return content == null || content.trim().isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("Zone[lignes %d-%d, centre: %d, lignes: %d]", 
                startLine, endLine, centerLine, lineCount);
        }
    }
}