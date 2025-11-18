package com.xml.lspserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.concurrent.CompletableFuture;

/**
 * Service TextDocument pour les notifications classiques LSP.
 * Les validations réelles sont déclenchées par la palette VSCode.
 */
public class XmlTextDocumentService implements TextDocumentService {


    private final XmlLanguageServer server;

    public XmlTextDocumentService(XmlLanguageServer server) {
        this.server = server;

    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {

    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.completedFuture(null);
    }



}