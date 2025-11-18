package com.xml.handlers;

import com.xml.models.*;
import org.eclipse.lsp4j.*;



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


    private final LargeXmlValidator largeValidator = new LargeXmlValidator();

    public CompletableFuture<ValidationResponse> validateFiles(ValidateFilesParams params) {
        

        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                File xsdFile = params.xsdUri != null ? new File(URI.create(params.xsdUri)) : null;

                if (!xmlFile.exists()) {
                    return new ValidationResponse(false,
                            "Fichier XML introuvable: " + xmlFile.getAbsolutePath());
                }



                // Validation optimisée
                LargeXmlValidator.ValidationResult result =
                        largeValidator.validateWithoutZones(xmlFile, xsdFile);

                // Conversion en diagnostics LSP
                List<Diagnostic> diagnostics = convertToDiagnostics(result.getErrors());

                // ✅ JUSTE RETOURNER LA RÉPONSE - Pas de publishDiagnostics !
                ValidationResponse response = new ValidationResponse();
                response.success = result.isSuccess();
                response.diagnostics = diagnostics;  // ← Les diagnostics sont dans la réponse
                response.errors = result.getErrors();
                response.fileSize = result.getFileSize();
                response.validationTime = result.getValidationTime();
                response.errorCount = result.getErrorCount();
                response.warningCount = result.getWarningCount();
                response.summary = result.getSummary();

                


                return response;

            } catch (Exception e) {

                return new ValidationResponse(false, "Erreur validation: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<NavigationResponse> navigateToError(NavigationParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                XMLError error = params.error;

                

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

                return new NavigationResponse(false, "Erreur navigation: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<ZoneExtractionResponse> extractErrorZones(ZoneExtractionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File xmlFile = new File(URI.create(params.xmlUri));
                List<Integer> errorIndexes = params.errorIndexes;

                

                // Extraire les zones uniquement pour les erreurs demandées
                List<XMLError> errorsWithZones = largeValidator.extractZonesForErrors(
                        params.errors, errorIndexes
                );

                return new ZoneExtractionResponse(true, errorsWithZones,
                        "Zones extraites pour " + errorIndexes.size() + " erreurs");

            } catch (Exception e) {

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

        return switch (severity.toLowerCase()) {
            case "error" -> DiagnosticSeverity.Error;
            case "warning" -> DiagnosticSeverity.Warning;
            case "info" -> DiagnosticSeverity.Information;
            case "hint" -> DiagnosticSeverity.Hint;
            default -> DiagnosticSeverity.Error;
        };
    }




}