package com.xml;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import com.google.gson.JsonObject;

/**
 * Interface client pour les notifications personnalisées
 */
@JsonSegment("xml")
public interface CustomLanguageClient {
    
    /**
     * Notification pour les résultats de validation
     */
    @JsonNotification("xml/validationResults")
    void sendValidationResults(ValidationResultsParams params);
    
    /**
     * Notification pour les erreurs de structure XML
     */
    @JsonNotification("xml/structureError")
    void sendStructureError(StructureErrorParams params);
}