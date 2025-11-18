package com.xml.lspserver;

import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;



import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Lanceur principal du serveur LSP - Version corrigée
 */
public class XmlLanguageServerLauncher {



    public static void main(String[] args) {
        try {
            

            // Configuration mémoire réduite
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            

            XmlLanguageServer server = new XmlLanguageServer();

            // Créer le launcher LSP - VERSION CORRIGÉE
            var launcher = LSPLauncher.createServerLauncher(
                    server,
                    System.in,
                    System.out
            );

            // Connecter le client APRÈS la création du launcher
            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);

            
            

            // Démarrer l'écoute
            Future<?> listening = launcher.startListening();

            // Attendre que l'écoute se termine
            listening.get();

        } catch (Exception e) {

            System.exit(1);
        }
    }
}