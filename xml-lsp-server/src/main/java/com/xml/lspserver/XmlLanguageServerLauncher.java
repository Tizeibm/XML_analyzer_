package com.xml;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lance le serveur LSP côté Java.
 * Le serveur écoute sur stdin/stdout (format JSON-RPC).
 */
public class XmlLanguageServerLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(XmlLanguageServerLauncher.class);

    public static void main(String[] args) throws Exception {
        LOG.info("Démarrage du serveur XML LSP");
        
        XmlLanguageServer server = new XmlLanguageServer();
        var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        
        server.connect(launcher.getRemoteProxy());
        LOG.info("Serveur connecté au client");
        
        launcher.startListening();
    }
}
