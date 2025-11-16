package com.xml;

import org.eclipse.lsp4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de validation avec positions précises
 */
public class XMLValidatorService {
    private static final Logger LOG = LoggerFactory.getLogger(XMLValidatorService.class);

    /**
     * Analyse avec calcul des positions précises
     */
    public ValidationResult analyzeWithPrecisePositions(File xmlFile, File xsdFile) {
        LOG.info("Analyse XML avec positions précises: {}", xmlFile.getAbsolutePath());

        ErrorCollector collector = new ErrorCollector();

        // Validation XSD si disponible
        boolean xsdValid = true;
        if (xsdFile != null && xsdFile.exists()) {
            xsdValid = new Validator(collector).validate(xmlFile, xsdFile);
        }

        // Parsing SAX/StAX pour la structure
        XMLParser parser = new XMLParser(collector);
        parser.parse(xmlFile);

        // Calculer les positions précises pour chaque erreur
        List<XMLError> errorsWithPrecisePositions = calculatePrecisePositions(
                collector.getErrors(), xmlFile
        );

        // Convertir en diagnostics LSP
        List<Diagnostic> diagnostics = convertToPreciseDiagnostics(errorsWithPrecisePositions, xmlFile);

        LOG.info("Analyse complétée: {} erreurs avec positions précises", errorsWithPrecisePositions.size());

        return new ValidationResult(true, diagnostics, errorsWithPrecisePositions);
    }

    /**
     * Calcule les positions précises pour toutes les erreurs
     */
    private List<XMLError> calculatePrecisePositions(List<XMLError> errors, File xmlFile) {
        List<XMLError> preciseErrors = new ArrayList<>();

        for (XMLError error : errors) {
            XMLError preciseError = calculatePrecisePosition(error, xmlFile);
            preciseErrors.add(preciseError);
        }

        return preciseErrors;
    }

    /**
     * Calcule la position précise d'une erreur spécifique
     */
    private XMLError calculatePrecisePosition(XMLError error, File xmlFile) {
        // Selon le type d'erreur, on utilise différentes stratégies
        switch (error.getType()) {
            case "STRUCTURE":
                if (error.getMessage().contains("non fermée") && error.getTagName() != null) {
                    Range preciseRange = PreciseErrorLocator.findUnclosedTagPosition(
                            xmlFile, error.getLineNumber(), error.getTagName()
                    );
                    error.setPrecisePosition(
                            preciseRange.getStart().getLine() + 1,
                            preciseRange.getStart().getCharacter() + 1,
                            preciseRange.getEnd().getLine() + 1,
                            preciseRange.getEnd().getCharacter() + 1
                    );
                }
                break;

            case "SYNTAX":
            case "VALIDATION_ERROR":
                if (error.getTagName() != null) {
                    Range preciseRange = PreciseErrorLocator.findExactTagPosition(
                            xmlFile, error.getLineNumber(), error.getTagName()
                    );
                    error.setPrecisePosition(
                            preciseRange.getStart().getLine() + 1,
                            preciseRange.getStart().getCharacter() + 1,
                            preciseRange.getEnd().getLine() + 1,
                            preciseRange.getEnd().getCharacter() + 1
                    );
                }
                break;
        }

        return error;
    }

    /**
     * Convertit les erreurs en diagnostics avec plages précises
     */
    private List<Diagnostic> convertToPreciseDiagnostics(List<XMLError> errors, File xmlFile) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (XMLError err : errors) {
            Diagnostic d = new Diagnostic();
            d.setSeverity(mapSeverity(err.getType()));
            d.setMessage(err.getMessage());
            d.setCode(err.getType());
            d.setSource("xml-validator");

            // Utiliser la position précise si disponible
            if (err.getPreciseStartLine() > 0) {
                d.setRange(new Range(
                        new Position(err.getPreciseStartLine() - 1, err.getPreciseStartColumn() - 1),
                        new Position(err.getPreciseEndLine() - 1, err.getPreciseEndColumn() - 1)
                ));
            } else {
                // Fallback à la position de ligne
                d.setRange(new Range(
                        new Position(err.getLineNumber() - 1, 0),
                        new Position(err.getLineNumber() - 1, 50)
                ));
            }

            diagnostics.add(d);
        }

        return diagnostics;
    }

    private DiagnosticSeverity mapSeverity(String type) {
        switch (type) {
            case "FATAL_SYNTAX":
            case "FATAL_VALIDATION":
            case "VALIDATION_ERROR":
            case "STRUCTURE":
                return DiagnosticSeverity.Error;
            case "VALIDATION_WARNING":
            case "WARNING":
                return DiagnosticSeverity.Warning;
            default:
                return DiagnosticSeverity.Information;
        }
    }

    /**
     * Résultat de validation avec toutes les données
     */
    public static class ValidationResult {
        public final boolean success;
        public final List<Diagnostic> diagnostics;
        public final List<XMLError> errors;
        public final long fileSize;
        public final long validationTime;

        public ValidationResult(boolean success, List<Diagnostic> diagnostics, List<XMLError> errors) {
            this.success = success;
            this.diagnostics = diagnostics;
            this.errors = errors;
            this.fileSize = 0; // Peut être calculé
            this.validationTime = System.currentTimeMillis(); // Simplifié
        }
    }
}