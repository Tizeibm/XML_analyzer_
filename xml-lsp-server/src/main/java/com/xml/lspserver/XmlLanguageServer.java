package com.xml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation du LanguageServer LSP.
 * Gère l'initialisation et les services texte/workspace.
 */
public class XmlLanguageServer implements LanguageServer {

    private static final Logger LOG = LoggerFactory.getLogger(XmlLanguageServer.class);
    
    private final XmlTextDocumentService textService;
    private final XmlWorkspaceService workspaceService;
    private LanguageClient client;

    public XmlLanguageServer() {
        this.textService = new XmlTextDocumentService(this);
        this.workspaceService = new XmlWorkspaceService(this);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        LOG.info("Initialisation du serveur LSP");
        
        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        
        caps.setExecuteCommandProvider(new ExecuteCommandOptions(
            java.util.Arrays.asList(
                "xml.validateFiles",
                "xml.patchFragment",
                "xml.getFragment"
            )
        ));
        
        InitializeResult result = new InitializeResult();
        result.setCapabilities(caps);
        
        return CompletableFuture.completedFuture(result);
    }

    @Override public void initialized(InitializedParams params) {
        LOG.info("Serveur initialisé");
    }

    @Override public CompletableFuture<Object> shutdown() {
        LOG.info("Shutdown");
        return CompletableFuture.completedFuture(null);
    }

    @Override public void exit() {
        LOG.info("Exit");
        System.exit(0);
    }

    @Override public TextDocumentService getTextDocumentService() { 
        return textService; 
    }

    @Override public WorkspaceService getWorkspaceService() { 
        return workspaceService; 
    }

    public void connect(LanguageClient client) { 
        this.client = client;
        LOG.info("Client connecté");
    }

    public LanguageClient getClient() { 
        return client; 
    }
}
