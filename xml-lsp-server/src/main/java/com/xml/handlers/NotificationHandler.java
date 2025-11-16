package com.xml;

import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Gère les notifications personnalisées pour les résultats de validation.
 */
public class NotificationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationHandler.class);
    private final LanguageClient client;
    private final CustomLanguageClient customClient;

    public NotificationHandler(LanguageClient client) {
        this.client = client;
        // Cast sécurisé vers l'interface personnalisée
        this.customClient = (CustomLanguageClient) client;
    }

    /**
     * Envoie les résultats complets de validation au client
     */
    public void sendValidationResults(String xmlUri, List<XMLError> errors) {
        try {
            JsonArray errorsArray = new JsonArray();
            int errorCount = 0;
            int warningCount = 0;
            
            for (XMLError error : errors) {
                errorsArray.add(errorToJson(error));
                
                if ("error".equals(error.getSeverity())) {
                    errorCount++;
                } else if ("warning".equals(error.getSeverity())) {
                    warningCount++;
                }
            }
            
            ValidationResultsParams params = new ValidationResultsParams(
                xmlUri, errorsArray, errorCount, warningCount
            );
            
            customClient.sendValidationResults(params);
            LOG.info("Notification de validation envoyée: {} erreurs, {} warnings", 
                    errorCount, warningCount);
                    
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi des résultats de validation", e);
        }
    }

    /**
     * Envoie une erreur de structure spécifique (balise non fermée, etc.)
     */
    public void sendStructureError(String xmlUri, XMLError error) {
        try {
            StructureErrorParams params = new StructureErrorParams(
                xmlUri,
                error.getMessage(),
                error.getLineNumber(),
                error.getColumn(),
                extractTagName(error.getMessage()),
                error.getType()
            );
            
            customClient.sendStructureError(params);
            LOG.info("Notification d'erreur structurelle envoyée: {} ligne {}", 
                    params.tagName, params.lineNumber);
                    
        } catch (Exception e) {
            LOG.error("Erreur lors de l'envoi de l'erreur structurelle", e);
        }
    }

    /**
     * Convertit un XMLError en JSON pour le client
     */
    public JsonObject errorToJson(XMLError error) {
        JsonObject json = new JsonObject();
        json.addProperty("id", error.getId());
        json.addProperty("line", error.getLineNumber());
        json.addProperty("column", error.getColumn());
        json.addProperty("message", error.getMessage());
        json.addProperty("severity", error.getSeverity());
        json.addProperty("code", error.getCode());
        
        json.addProperty("fragment", error.getFragment());
        json.addProperty("fragmentStartLine", error.getFragmentStartLine());
        json.addProperty("fragmentEndLine", error.getFragmentEndLine());
        
        // Informations supplémentaires pour le client
        json.addProperty("tagName", extractTagName(error.getMessage()));
        json.addProperty("suggestion", generateSuggestion(error));
        
        return json;
    }

    private String extractTagName(String message) {
        if (message == null) return "";
        
        // Extraire le nom de balise des messages d'erreur
        if (message.contains("<") && message.contains(">")) {
            int start = message.indexOf('<');
            int end = message.indexOf('>', start);
            if (end > start) {
                String tag = message.substring(start + 1, end).trim();
                // Enlever les attributs éventuels
                int spaceIndex = tag.indexOf(' ');
                if (spaceIndex > 0) {
                    return tag.substring(0, spaceIndex);
                }
                return tag;
            }
        }
        
        // Fallback: chercher des motifs communs
        if (message.contains("balise")) {
            String[] parts = message.split("balise");
            if (parts.length > 1) {
                String part = parts[1].trim();
                if (part.startsWith("<")) {
                    return extractTagName(part);
                }
            }
        }
        
        return "";
    }

    private String generateSuggestion(XMLError error) {
        switch (error.getType()) {
            case "STRUCTURE":
                if (error.getMessage().contains("non fermée")) {
                    return "Ajouter la balise fermante correspondante";
                } else if (error.getMessage().contains("inattendue")) {
                    return "Vérifier l'ordre des balises fermantes";
                }
                break;
            case "SYNTAX":
                return "Vérifier la syntaxe XML";
            case "VALIDATION_ERROR":
                return "Consulter le schéma XSD pour les contraintes";
            default:
                return "";
        }
        return "";
    }
}