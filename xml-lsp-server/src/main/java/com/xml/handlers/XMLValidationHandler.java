package com.xml.handlers;

import com.xml.models.ValidateFilesParams;
import com.xml.models.XMLError;
import org.eclipse.lsp4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handler LSP optimisé pour les gros fichiers XML
 * Utilise publishDiagnostics pour envoyer les résultats
 */
public class XMLValidationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(XMLValidationHandler.class);

    private final LargeXmlValidator largeValidator = new LargeXmlValidator();
    private org.eclipse.lsp4j.services.LanguageClient client;

    public void setClient(org.eclipse.lsp4j.services.LanguageClient client) {
        this.client = client;
    }

    public CompletableFuture<ValidationResponse> validateFiles(ValidateFilesParams params) {
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
                List<Diagnostic> diagnostics = convertToDiagnostics(result.getErrors());

                // ENVOI DES DIAGNOSTICS via publishDiagnostics
                if (client != null) {
                    PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
                    diagnosticsParams.setUri(params.xmlUri);
                    diagnosticsParams.setDiagnostics(diagnostics);

                    client.publishDiagnostics(diagnosticsParams);
                    LOG.info("✅ Diagnostics publiés: {} erreurs, {} warnings",
                            result.getErrorCount(), result.getWarningCount());
                } else {
                    LOG.warn("⚠️ Client non disponible, impossible de publier les diagnostics");
                }

                // Réponse avec métriques
                ValidationResponse response = new ValidationResponse();
                response.success = result.isSuccess();
                response.diagnostics = diagnostics;
                response.errors = result.getErrors();
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

                // En cas d'erreur, publier un diagnostic d'erreur
                if (client != null) {
                    List<Diagnostic> errorDiagnostics = new ArrayList<>();
                    Diagnostic errorDiag = new Diagnostic();
                    errorDiag.setRange(new Range(new Position(0, 0), new Position(0, 1)));
                    errorDiag.setSeverity(DiagnosticSeverity.Error);
                    errorDiag.setMessage("Erreur de validation: " + e.getMessage());
                    errorDiag.setSource("xml-validator");
                    errorDiagnostics.add(errorDiag);

                    PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
                    diagnosticsParams.setUri(params.xmlUri);
                    diagnosticsParams.setDiagnostics(errorDiagnostics);
                    client.publishDiagnostics(diagnosticsParams);
                }

                return new ValidationResponse(false, "Erreur validation: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<NavigationResponse> navigateToError(NavigationParams params) {
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

    public CompletableFuture<ZoneExtractionResponse> extractErrorZones(ZoneExtractionParams params) {
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

    public CompletableFuture<PatchResponse> patchFragment(PatchParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean success = FilePatcher.patchFile(
                        new File(URI.create(params.xmlUri)).getAbsolutePath(),
                        params.modifiedFragment,
                        params.fragmentStartLine,
                        params.fragmentEndLine
                );

                return new PatchResponse(success,
                        success ? "Patch appliqué avec succès" : "Échec du patching");

            } catch (Exception e) {
                LOG.error("Erreur patching", e);
                return new PatchResponse(false, "Erreur: " + e.getMessage());
            }
        });
    }

    /**
     * Convertit les XMLError en Diagnostics LSP standard
     */
    private List<Diagnostic> convertToDiagnostics(List<XMLError> errors) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (XMLError error : errors) {
            Diagnostic diagnostic = new Diagnostic();

            // Position (LSP utilise des indices 0-based)
            int line = Math.max(0, error.getLineNumber() - 1);
            int column = Math.max(0, error.getColumn() - 1);

            Range range = new Range(
                    new Position(line, column),
                    new Position(line, column + 30) // Zone de soulignement approximative
            );
            diagnostic.setRange(range);

            // Sévérité
            diagnostic.setSeverity(mapSeverity(error.getSeverity()));

            // Message
            diagnostic.setMessage(error.getMessage());

            // Code et source
            diagnostic.setCode(error.getCode());
            diagnostic.setSource("xml-validator");

            // Tags optionnels
            if (error.getType().equals("WARNING")) {
                diagnostic.setTags(List.of(DiagnosticTag.Unnecessary));
            }

            diagnostics.add(diagnostic);
        }

        return diagnostics;
    }

    private DiagnosticSeverity mapSeverity(String severity) {
        if (severity == null) return DiagnosticSeverity.Error;

        switch (severity.toLowerCase()) {
            case "error":
                return DiagnosticSeverity.Error;
            case "warning":
                return DiagnosticSeverity.Warning;
            case "info":
                return DiagnosticSeverity.Information;
            case "hint":
                return DiagnosticSeverity.Hint;
            default:
                return DiagnosticSeverity.Error;
        }
    }

    // Classes de paramètres et réponses (inchangées)
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

    public static class PatchParams {
        public String xmlUri;
        public String modifiedFragment;
        public int fragmentStartLine;
        public int fragmentEndLine;
    }

    public static class PatchResponse {
        public boolean success;
        public String message;

        public PatchResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class ValidationResponse {
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
}