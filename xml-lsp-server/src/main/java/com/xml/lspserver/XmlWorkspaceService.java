package com.xml.lspserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;



/**
 * Service Workspace pour les notifications de configuration et fichiers surveillés.
 */
public class XmlWorkspaceService implements WorkspaceService {



    public XmlWorkspaceService(XmlLanguageServer server) {}

    @Override 
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        
    }

    @Override 
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        
    }


}