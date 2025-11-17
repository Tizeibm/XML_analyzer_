package com.xml.lspserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service TextDocument pour les notifications classiques LSP.
 * Les validations réelles sont déclenchées par la palette VSCode.
 */
public class XmlTextDocumentService implements TextDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(XmlTextDocumentService.class);
    private final XmlLanguageServer server;


    public XmlTextDocumentService(XmlLanguageServer server) {
        this.server = server;

    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        LOG.info("Document ouvert : {}", params.getTextDocument().getUri());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        LOG.debug("Document modifié : {}", params.getTextDocument().getUri());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        LOG.info("Document fermé : {}", params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        LOG.info("Document sauvegardé : {}", params.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.completedFuture(null);
    }



}