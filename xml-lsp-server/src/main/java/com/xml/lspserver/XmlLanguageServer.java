package com.xml.lspserver;

import com.xml.handlers.XMLValidationHandler;
import com.xml.models.ValidateFilesParams;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Serveur LSP principal qui expose directement les méthodes personnalisées
 */
public class XmlLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger LOG = LoggerFactory.getLogger(XmlLanguageServer.class);

    private final XmlTextDocumentService textService;
    private final XmlWorkspaceService workspaceService;
    private final XMLValidationHandler validationHandler;
    private LanguageClient client;

    public XmlLanguageServer() {
        this.textService = new XmlTextDocumentService(this);
        this.workspaceService = new XmlWorkspaceService(this);
        this.validationHandler = new XMLValidationHandler();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        LOG.info("🚀 Initialisation du serveur LSP XML");

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

        LOG.info("✅ Capacités du serveur configurées");
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params) {
        LOG.info("✅ Serveur LSP complètement initialisé");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        LOG.info("Arrêt du serveur LSP");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        LOG.info("Extinction du serveur LSP");
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
        LOG.info("✅ Client LSP connecté");
    }

    public LanguageClient getClient() {
        return client;
    }

    /**
     * Méthodes personnalisées exposées directement depuis le serveur principal
     */

    @JsonRequest("xml/validateFiles")
    public CompletableFuture<XMLValidationHandler.ValidationResponse> validateFiles(ValidateFilesParams params) {
        LOG.info("📥 Requête reçue: xml/validateFiles");
        LOG.info("🎯🎯🎯 MÉTHODE validateFiles APPELÉE AVEC SUCCÈS !");
        LOG.info("🎯 XML: {}, XSD: {}", params.xmlUri, params.xsdUri);
        return validationHandler.validateFiles(params);
    }

    @JsonRequest("xml/navigateToError")
    public CompletableFuture<XMLValidationHandler.NavigationResponse> navigateToError(XMLValidationHandler.NavigationParams params) {
        LOG.info("📥 Requête reçue: xml/navigateToError");
        return validationHandler.navigateToError(params);
    }

    @JsonRequest("xml/patchFragment")
    public CompletableFuture<XMLValidationHandler.PatchResponse> patchFragment(XMLValidationHandler.PatchParams params) {
        LOG.info("📥 Requête reçue: xml/patchFragment");
        return validationHandler.patchFragment(params);
    }

    @JsonRequest("xml/extractErrorZones")
    public CompletableFuture<XMLValidationHandler.ZoneExtractionResponse> extractErrorZones(XMLValidationHandler.ZoneExtractionParams params) {
        LOG.info("📥 Requête reçue: xml/extractErrorZones");
        return validationHandler.extractErrorZones(params);
    }
}