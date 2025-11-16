package com.xml;

import org.eclipse.lsp4j.Diagnostic;
import java.util.List;

/**
 * Résultats de validation renvoyés au client LSP.
 * Contient les diagnostiques à afficher dans l'éditeur.
 */
public class ValidationResults {
    public String uri;
    public List<Diagnostic> diagnostics;

    public ValidationResults(String uri, List<Diagnostic> diagnostics) {
        this.uri = uri;
        this.diagnostics = diagnostics;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(List<Diagnostic> diagnostics) {
        this.diagnostics = diagnostics;
    }
}
