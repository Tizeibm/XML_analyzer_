package com.xml;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handler LSP optimisé pour les gros fichiers XML
 */
public class XMLValidationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(XMLValidationHandler.class);

    private final LargeXmlValidator largeValidator = new LargeXmlValidator();

    @JsonRequest("xml/validateFiles")
    public CompletableFuture<ValidationResponse> onValidateFiles(ValidateFilesParams params) {
        LOG.info("📁 Validation demandée - XML: {}, XSD: {}", params.xmlUri, params.xsdUri);

        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                File xsdFile = params.xsdUri != null ? new File(URI.create(params.xsdUri)) : null;

                if (!xmlFile.exists()) {
                    return new ValidationResponse(false,
                            "Fichier XML introuvable: " + xmlFile.getAbsolutePath());
                }

                LOG.info("🔍 Analyse du fichier: {} ({} MB)",
                        xmlFile.getName(), xmlFile.length() / (1024 * 1024));

                // Validation optimisée sans extraction de zones
                LargeXmlValidator.ValidationResult result =
                        largeValidator.validateWithoutZones(xmlFile, xsdFile);

                // Conversion en diagnostics LSP
                List<org.eclipse.lsp4j.Diagnostic> diagnostics =
                        convertToDiagnostics(result.getErrors());

                // Réponse avec métriques
                ValidationResponse response = new ValidationResponse();
                response.success = result.isSuccess();
                response.diagnostics = diagnostics;
                response.errors = result.getErrors(); // Sans zones pour l'instant
                response.fileSize = result.getFileSize();
                response.validationTime = result.getValidationTime();
                response.errorCount = result.getErrorCount();
                response.warningCount = result.getWarningCount();
                response.summary = result.getSummary();

                LOG.info("✅ {} - {} erreurs, {} warnings",
                        response.summary, response.errorCount, response.warningCount);

                return response;

            } catch (Exception e) {
                LOG.error("❌ Erreur validation: {}", e.getMessage(), e);
                return new ValidationResponse(false, "Erreur validation: " + e.getMessage());
            }
        });
    }

    /**
     * Extrait les zones pour des erreurs spécifiques (à la demande)
     */
    @JsonRequest("xml/extractErrorZones")
    public CompletableFuture<ZoneExtractionResponse> onExtractErrorZones(ZoneExtractionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                List<Integer> errorIndexes = params.errorIndexes;

                LOG.info("📦 Extraction zones pour {} erreurs dans {}", errorIndexes.size(), xmlFile.getName());

                // Extraire les zones uniquement pour les erreurs demandées
                List<XMLError> errorsWithZones = largeValidator.extractZonesForErrors(
                        params.errors, errorIndexes
                );

                return new ZoneExtractionResponse(true, errorsWithZones,
                        "Zones extraites pour " + errorIndexes.size() + " erreurs");

            } catch (Exception e) {
                LOG.error("Erreur extraction zones: {}", e.getMessage());
                return new ZoneExtractionResponse(false, params.errors,
                        "Erreur extraction: " + e.getMessage());
            }
        });
    }

    /**
     * Navigation précise vers une erreur avec extraction de zone
     */
    @JsonRequest("xml/navigateToError")
    public CompletableFuture<NavigationResponse> onNavigateToError(NavigationParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                XMLError error = params.error;

                LOG.info("🧭 Navigation vers erreur {} dans {}", error.getLineNumber(), xmlFile.getName());

                // Extraire la zone pour cette erreur spécifique
                XMLError errorWithZone = largeValidator.extractZoneForError(error);

                // Préparer la réponse de navigation
                NavigationResponse response = new NavigationResponse();
                response.success = true;
                response.error = errorWithZone;
                response.preciseRange = errorWithZone.getPreciseRangeJson();
                response.hasZone = errorWithZone.isZoneExtracted();

                if (response.hasZone) {
                    response.zoneContent = errorWithZone.getZoneContent();
                    response.zoneStartLine = errorWithZone.getZoneStartLine();
                    response.zoneEndLine = errorWithZone.getZoneEndLine();
                }

                return response;

            } catch (Exception e) {
                LOG.error("Erreur navigation: {}", e.getMessage());
                return new NavigationResponse(false, "Erreur navigation: " + e.getMessage());
            }
        });
    }

    // Conversion des erreurs en diagnostics LSP
    private List<org.eclipse.lsp4j.Diagnostic> convertToDiagnostics(List<XMLError> errors) {
        List<org.eclipse.lsp4j.Diagnostic> diagnostics = new ArrayList<>();

        for (XMLError error : errors) {
            org.eclipse.lsp4j.Diagnostic diagnostic = new org.eclipse.lsp4j.Diagnostic();

            // Sévérité
            diagnostic.setSeverity(mapSeverity(error.getSeverity()));
            diagnostic.setMessage(error.getMessage());
            diagnostic.setCode(error.getType());
            diagnostic.setSource("xml-validator-gros-fichiers");

            // Plage précise
            diagnostic.setRange(new org.eclipse.lsp4j.Range(
                    new org.eclipse.lsp4j.Position(error.getPreciseStartLine() - 1, error.getPreciseStartColumn() - 1),
                    new org.eclipse.lsp4j.Position(error.getPreciseEndLine() - 1, error.getPreciseEndColumn() - 1)
            ));

            diagnostics.add(diagnostic);
        }

        return diagnostics;
    }

    private org.eclipse.lsp4j.DiagnosticSeverity mapSeverity(String severity) {
        switch (severity) {
            case "error": return org.eclipse.lsp4j.DiagnosticSeverity.Error;
            case "warning": return org.eclipse.lsp4j.DiagnosticSeverity.Warning;
            default: return org.eclipse.lsp4j.DiagnosticSeverity.Information;
        }
    }

    // Classes de paramètres et réponses
    public static class ZoneExtractionParams {
        public String xmlUri;
        public List<XMLError> errors;
        public List<Integer> errorIndexes;
    }

    public static class ZoneExtractionResponse {
        public boolean success;
        public List<XMLError> errors;
        public String message;

        public ZoneExtractionResponse(boolean success, List<XMLError> errors, String message) {
            this.success = success;
            this.errors = errors;
            this.message = message;
        }
    }

    public static class NavigationParams {
        public String xmlUri;
        public XMLError error;
    }

    public static class NavigationResponse {
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

    // Classe ValidationResponse existante (à adapter)
    public static class ValidationResponse {
        public boolean success;
        public String message;
        public java.util.List<org.eclipse.lsp4j.Diagnostic> diagnostics;
        public java.util.List<XMLError> errors;
        public long fileSize;
        public long validationTime;
        public int errorCount;
        public int warningCount;
        public String summary;

        public ValidationResponse() {}

        public ValidationResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.diagnostics = java.util.List.of();
            this.errors = java.util.List.of();
        }
    }
}