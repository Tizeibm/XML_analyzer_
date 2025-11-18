package com.xml.lspserver;

import com.xml.handlers.XMLValidationHandler;
import com.xml.models.*;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;



import java.util.concurrent.CompletableFuture;

/**
 * Serveur LSP principal qui expose directement les méthodes personnalisées
 */
@JsonSegment("xml")
public class XmlLanguageServer implements LanguageServer, LanguageClientAware {



    private final XmlTextDocumentService textService;
    private final XmlWorkspaceService workspaceService;
    private final XMLValidationHandler validationHandler = new XMLValidationHandler();
    private LanguageClient client;

    public XmlLanguageServer() {
        this.textService = new XmlTextDocumentService(this);
        this.workspaceService = new XmlWorkspaceService(this);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        

        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);

        // Activer l'exécution de commandes
        caps.setExecuteCommandProvider(new ExecuteCommandOptions(
                java.util.Arrays.asList(
                        "xml.validateFiles",
                        "xml.navigateToError",
                        "xml.patchFragment"
                )
        ));

        InitializeResult result = new InitializeResult();
        result.setCapabilities(caps);

        
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params) {
        
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        
    }

    public LanguageClient getClient() {
        return client;
    }

    /**
     * Méthodes personnalisées exposées directement depuis le serveur principal
     */

    @JsonRequest("validateFiles")
    public CompletableFuture<ValidationResponse> validateFiles(ValidateFilesParams params) {
        
        
        
        return validationHandler.validateFiles(params);
    }

    @JsonRequest("navigateToError")
    public CompletableFuture<NavigationResponse> navigateToError(NavigationParams params) {
        
        return validationHandler.navigateToError(params);
    }

    @JsonRequest("patchFragment")
    public CompletableFuture<PatchResponse> patchFragment(PatchParams params) {
        
        return validationHandler.patchFragment(params);
    }

    @JsonRequest("extractErrorZones")
    public CompletableFuture<ZoneExtractionResponse> extractErrorZones(ZoneExtractionParams params) {
        
        return validationHandler.extractErrorZones(params);
    }


}