package com.xml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Workspace pour les notifications de configuration et fichiers surveillés.
 */
public class XmlWorkspaceService implements WorkspaceService {

    private static final Logger LOG = LoggerFactory.getLogger(XmlWorkspaceService.class);

    public XmlWorkspaceService(XmlLanguageServer server) {}

    @Override 
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        LOG.info("Configuration modifiée");
    }

    @Override 
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        LOG.info("Fichiers surveillés modifiés : {}", params.getChanges().size());
    }


}
