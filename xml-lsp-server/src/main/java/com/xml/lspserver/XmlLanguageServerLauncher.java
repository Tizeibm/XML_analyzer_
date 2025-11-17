package com.xml.lspserver;

import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Lanceur principal du serveur LSP - Version corrigée
 */
public class XmlLanguageServerLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(XmlLanguageServerLauncher.class);

    public static void main(String[] args) {
        try {
            LOG.info("🚀 Démarrage du serveur XML LSP...");

            // Configuration mémoire réduite
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            LOG.info("Mémoire max disponible: {} MB", maxMemory);

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

            LOG.info("✅ Serveur LSP démarré avec succès");
            LOG.info("📡 En attente de requêtes client...");

            // Démarrer l'écoute
            Future<?> listening = launcher.startListening();

            // Attendre que l'écoute se termine
            listening.get();

        } catch (Exception e) {
            LOG.error("❌ Erreur critique lors du démarrage du serveur", e);
            System.exit(1);
        }
    }
}