package com.xml.handlers;

import com.xml.Validator;
import com.xml.XMLParser;
import com.xml.models.ErrorCollector;
import com.xml.models.XMLError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de validation optimisé pour les très gros fichiers XML
 * N'extrait les zones que sur demande pour économiser la mémoire
 */
public class LargeXmlValidator {
    private static final Logger LOG = LoggerFactory.getLogger(LargeXmlValidator.class);
    
    private final ErrorCollector errorCollector;
    private File currentXmlFile;

    public LargeXmlValidator() {
        this.errorCollector = new ErrorCollector();
    }

    /**
     * Validation initiale sans extraction de zones (pour performance)
     */
    public ValidationResult validateWithoutZones(File xmlFile, File xsdFile) {
        if (!xmlFile.exists()){
            System.exit(0);
        }
        long startTime = System.currentTimeMillis();
        this.currentXmlFile = xmlFile;
        
        LOG.info("🚀 Début validation rapide pour gros fichier: {} ({} bytes)", 
                xmlFile.getName(), xmlFile.length());
        
        errorCollector.clear();

        // Validation XSD si disponible
        boolean xsdValid = true;
        if (xsdFile != null && xsdFile.exists()) {
            xsdValid = new Validator(errorCollector).validate(xmlFile, xsdFile);
        }

        // Parsing structurel (StAX streaming)
        XMLParser parser = new XMLParser(errorCollector);
        parser.parse(xmlFile);

        // Conversion en erreurs sans zones (économie mémoire)
        List<XMLError> errors = convertToErrorsWithoutZones(errorCollector.getErrors());
        
        long validationTime = System.currentTimeMillis() - startTime;
        LOG.info("✅ Validation rapide terminée en {}ms - {} erreurs", validationTime, errors.size());
        
        return new ValidationResult(xsdValid, errors, validationTime, xmlFile.length());
    }

    /**
     * Validation ultra-optimisée pour mémoire limitée
     */
    public ValidationResult validateWithMemoryOptimization(File xmlFile, File xsdFile) {
        // Nettoyer la mémoire avant validation
        System.gc();

        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        LOG.info("Mémoire utilisée avant validation: {} MB", startMemory / (1024 * 1024));

        ValidationResult result = validateWithoutZones(xmlFile, xsdFile);

        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        LOG.info("Mémoire utilisée après validation: {} MB", endMemory / (1024 * 1024));

        return result;
    }

    /**
     * Extrait les zones pour des erreurs spécifiques (à la demande)
     */
    public List<XMLError> extractZonesForErrors(List<XMLError> errors, List<Integer> errorIndexes) {
        if (currentXmlFile == null || !currentXmlFile.exists()) {
            LOG.warn("Aucun fichier XML courant pour extraction de zones");
            return errors;
        }
        
        long startTime = System.currentTimeMillis();
        
        for (int index : errorIndexes) {
            if (index >= 0 && index < errors.size()) {
                XMLError error = errors.get(index);
                if (!error.isZoneExtracted()) {
                    // Extraire la zone pour cette erreur spécifique
                    XmlZoneExtractor.XmlZone zone = XmlZoneExtractor.extractErrorZone(
                        currentXmlFile, error.getLineNumber()
                    );
                    
                    error.setZone(zone.getContent(), zone.getStartLine(), zone.getEndLine());
                    
                    LOG.debug("Zone extraite pour erreur {}: lignes {}-{}", 
                             index, zone.getStartLine(), zone.getEndLine());
                }
            }
        }
        
        long extractTime = System.currentTimeMillis() - startTime;
        LOG.info("📦 Extraction de {} zones en {}ms", errorIndexes.size(), extractTime);
        
        return errors;
    }

    /**
     * Extrait toutes les zones (à utiliser avec précaution pour gros fichiers)
     */
    public List<XMLError> extractAllZones(List<XMLError> errors) {
        if (currentXmlFile == null) {
            return errors;
        }
        
        LOG.info("Extraction de toutes les zones ({} erreurs)", errors.size());
        
        for (XMLError error : errors) {
            if (!error.isZoneExtracted()) {
                XmlZoneExtractor.XmlZone zone = XmlZoneExtractor.extractErrorZone(
                    currentXmlFile, error.getLineNumber()
                );
                error.setZone(zone.getContent(), zone.getStartLine(), zone.getEndLine());
            }
        }
        
        return errors;
    }

    /**
     * Extrait la zone pour une erreur spécifique
     */
    public XMLError extractZoneForError(XMLError error) {
        if (currentXmlFile != null && !error.isZoneExtracted()) {
            XmlZoneExtractor.XmlZone zone = XmlZoneExtractor.extractErrorZone(
                currentXmlFile, error.getLineNumber()
            );
            error.setZone(zone.getContent(), zone.getStartLine(), zone.getEndLine());
        }
        return error;
    }

    private List<XMLError> convertToErrorsWithoutZones(List<XMLError> originalErrors) {
        // On garde les erreurs mais sans les zones pour économiser la mémoire
        List<XMLError> lightweightErrors = new ArrayList<>();
        
        for (XMLError error : originalErrors) {
            // Créer une version légère sans zone
            XMLError lightweight = new XMLError(error.getMessage(), error.getLineNumber(), error.getType());
            lightweight.setColumn(error.getColumn());
            lightweight.setTagName(error.getTagName());
            lightweight.setPrecisePosition(
                error.getPreciseStartLine(), error.getPreciseStartColumn(),
                error.getPreciseEndLine(), error.getPreciseEndColumn()
            );
            
            lightweightErrors.add(lightweight);
        }
        
        return lightweightErrors;
    }

    /**
     * Résultat de validation optimisé
     */
    public static class ValidationResult {
        private final boolean success;
        private final List<XMLError> errors;
        private final long validationTime;
        private final long fileSize;
        private final int errorCount;
        private final int warningCount;

        public ValidationResult(boolean success, List<XMLError> errors, long validationTime, long fileSize) {
            this.success = success;
            this.errors = errors;
            this.validationTime = validationTime;
            this.fileSize = fileSize;
            
            // Compter erreurs et warnings
            int errCount = 0;
            int warnCount = 0;
            for (XMLError error : errors) {
                if ("error".equals(error.getSeverity())) {
                    errCount++;
                } else if ("warning".equals(error.getSeverity())) {
                    warnCount++;
                }
            }
            this.errorCount = errCount;
            this.warningCount = warnCount;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public List<XMLError> getErrors() { return errors; }
        public long getValidationTime() { return validationTime; }
        public long getFileSize() { return fileSize; }
        public int getErrorCount() { return errorCount; }
        public int getWarningCount() { return warningCount; }
        
        public String getSummary() {
            return String.format("Fichier: %d MB, Temps: %dms, Erreurs: %d, Warnings: %d",
                fileSize / (1024 * 1024), validationTime, errorCount, warningCount);
        }
    }
}