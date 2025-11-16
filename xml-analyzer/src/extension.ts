import * as path from 'path';
import * as fs from 'fs';
import * as vscode from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient/node';

let client: LanguageClient;
let extensionContext: vscode.ExtensionContext;

// Interfaces (garder les mêmes que précédemment)
interface ValidationResultsParams {
    xmlUri: string;
    errors: XMLError[];
    errorCount: number;
    warningCount: number;
}

interface StructureErrorParams {
    xmlUri: string;
    message: string;
    lineNumber: number;
    columnNumber: number;
    tagName: string;
    errorType: string;
}

interface XMLError {
    id: string;
    line: number;
    column: number;
    message: string;
    severity: string;
    code: string;
    fragment?: string;
    fragmentStartLine?: number;
    fragmentEndLine?: number;
    tagName?: string;
    suggestion?: string;
}

interface PatchFragmentParams {
    xmlUri: string;
    fragment: string;
    startLine: number;
    endLine: number;
}

interface ValidationResponse {
    diagnostics: any[];
    success?: boolean;
}

export function activate(context: vscode.ExtensionContext) {
    extensionContext = context;
    console.log('Extension XML Analyzer activée');

    // Commandes (garder les mêmes)
    const validateFileCommand = vscode.commands.registerCommand('xml.validateFile', async () => {
        const activeEditor = vscode.window.activeTextEditor;
        if (!activeEditor || activeEditor.document.languageId !== 'xml') {
            vscode.window.showWarningMessage('Ouvrez un fichier XML pour le valider');
            return;
        }
        await validateXmlFile(activeEditor.document.uri, null);
    });

    const validateWithXSDCommand = vscode.commands.registerCommand('xml.validateFileWithXSD', async () => {
        const activeEditor = vscode.window.activeTextEditor;
        if (!activeEditor || activeEditor.document.languageId !== 'xml') {
            vscode.window.showWarningMessage('Ouvrez un fichier XML pour le valider');
            return;
        }

        const xsdUri = await vscode.window.showOpenDialog({
            title: 'Sélectionner le schéma XSD',
            filters: { 'XSD': ['xsd'] },
            canSelectMany: false
        });

        if (xsdUri && xsdUri[0]) {
            await validateXmlFile(activeEditor.document.uri, xsdUri[0]);
        }
    });

    context.subscriptions.push(validateFileCommand, validateWithXSDCommand);

    // Démarrer le client LSP
    startLanguageClient(context);
}

async function startLanguageClient(context: vscode.ExtensionContext) {
    const config = vscode.workspace.getConfiguration('xmlLSP');
    let serverPath = config.get<string>('serverPath') || './server/xml-lsp-server.jar';
    const javaPath = config.get<string>('javaPath') || 'java';

    // Résoudre le chemin absolu du serveur
    if (!path.isAbsolute(serverPath)) {
        serverPath = path.resolve(context.extensionPath, serverPath);
    }

    // Vérifier que le JAR existe
    if (!fs.existsSync(serverPath)) {
        vscode.window.showErrorMessage(
            `Fichier serveur LSP introuvable: ${serverPath}. ` +
            `Veuillez configurer le chemin correct dans les paramètres.`,
            'Ouvrir les paramètres'
        ).then(selection => {
            if (selection === 'Ouvrir les paramètres') {
                vscode.commands.executeCommand('workbench.action.openSettings', 'xmlLSP.serverPath');
            }
        });
        return;
    }

    console.log(`Utilisation du serveur LSP: ${serverPath}`);

    const serverOptions: ServerOptions = {
        command: javaPath,
        args: ['-jar', serverPath],
        options: {
            cwd: context.extensionPath
        }
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'xml' }],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.xml')
        }
    };

    client = new LanguageClient(
        'xmlLSP',
        'XML Language Server',
        serverOptions,
        clientOptions
    );

    // Enregistrer les gestionnaires de notifications
    registerCustomNotifications();

    try {
        await client.start();
        console.log('Client LSP XML démarré avec succès');
        vscode.window.showInformationMessage('XML Analyzer démarré');
    } catch (error) {
        console.error('Erreur démarrage client LSP:', error);
        vscode.window.showErrorMessage(`Erreur démarrage XML Analyzer: ${error}`);
    }
}

function registerCustomNotifications() {
    client.onNotification('xml/validationResults', (params: ValidationResultsParams) => {
        handleValidationResults(params);
    });

    client.onNotification('xml/structureError', (params: StructureErrorParams) => {
        handleStructureError(params);
    });
}

async function validateXmlFile(xmlUri: vscode.Uri, xsdUri: vscode.Uri | null) {
    if (!client) {
        vscode.window.showErrorMessage('Client LSP non initialisé');
        return;
    }

    try {
        await vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: 'Validation XML en cours...',
            cancellable: false
        }, async (progress) => {
            const params = {
                xmlUri: xmlUri.toString(),
                xsdUri: xsdUri ? xsdUri.toString() : null
            };

            const result = await client.sendRequest('xml/validateFiles', params) as ValidationResponse;
            
            if (result.diagnostics && result.diagnostics.length > 0) {
                vscode.window.showInformationMessage(
                    `Validation terminée: ${result.diagnostics.length} problème(s) trouvé(s)`,
                    'Voir les détails'
                ).then(selection => {
                    if (selection === 'Voir les détails') {
                        showValidationPanel();
                    }
                });
            } else {
                vscode.window.showInformationMessage('✅ Aucune erreur trouvée dans le fichier XML');
            }

            return result;
        });

    } catch (error) {
        console.error('Erreur validation XML:', error);
        vscode.window.showErrorMessage(`Erreur lors de la validation: ${error}`);
    }
}

function handleValidationResults(params: ValidationResultsParams) {
    console.log('Résultats validation reçus:', params.errorCount, 'erreurs');
    
    if (params.errorCount > 0) {
        vscode.window.showWarningMessage(
            `Validation XML: ${params.errorCount} erreur(s), ${params.warningCount} avertissement(s)`,
            'Afficher le rapport'
        ).then(selection => {
            if (selection === 'Afficher le rapport') {
                showValidationResultsWebview(params);
            }
        });
    }

    extensionContext.workspaceState.update('lastValidationResults', params);
}

function handleStructureError(params: StructureErrorParams) {
    const message = `Erreur structurelle (ligne ${params.lineNumber}): ${params.message}`;
    vscode.window.showErrorMessage(message, 'Corriger automatiquement')
        .then(async (selection) => {
            if (selection === 'Corriger automatiquement') {
                await attemptAutoFix(params);
            }
        });
}

async function attemptAutoFix(params: StructureErrorParams) {
    try {
        const result = await client.sendRequest('xml/autoFix', {
            xmlUri: params.xmlUri,
            errorType: params.errorType,
            lineNumber: params.lineNumber,
            tagName: params.tagName
        }) as { success: boolean };

        if (result.success) {
            vscode.window.showInformationMessage('Correctif appliqué avec succès');
        } else {
            vscode.window.showWarningMessage('Impossible d\'appliquer le correctif automatiquement');
        }
    } catch (error) {
        vscode.window.showErrorMessage(`Erreur lors de l'application du correctif: ${error}`);
    }
}

function showValidationPanel() {
    const results = extensionContext.workspaceState.get<ValidationResultsParams>('lastValidationResults');
    if (results) {
        showValidationResultsWebview(results);
    } else {
        vscode.window.showInformationMessage('Aucun résultat de validation disponible');
    }
}

function showValidationResultsWebview(params: ValidationResultsParams) {
    const panel = vscode.window.createWebviewPanel(
        'xmlValidationResults',
        `Validation XML - ${path.basename(params.xmlUri)}`,
        vscode.ViewColumn.Two,
        { enableScripts: true }
    );

    panel.webview.html = generateValidationResultsHtml(params);
    
    panel.webview.onDidReceiveMessage(
        async (message) => {
            switch (message.command) {
                case 'showError':
                    await showErrorInEditor(message.line, message.message);
                    break;
                case 'validateAgain':
                    const xmlUri = vscode.Uri.parse(params.xmlUri);
                    await validateXmlFile(xmlUri, null);
                    break;
            }
        },
        undefined,
        extensionContext.subscriptions
    );
}

function escapeHtml(unsafe: string): string {
    if (!unsafe) return '';
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function generateValidationResultsHtml(params: ValidationResultsParams): string {
    return `
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Résultats Validation XML</title>
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; 
                   background-color: var(--vscode-editor-background); color: var(--vscode-editor-foreground); }
            .header { border-bottom: 1px solid var(--vscode-panel-border); padding-bottom: 15px; margin-bottom: 20px; }
            .summary { display: flex; gap: 20px; margin-bottom: 15px; }
            .summary-item { padding: 10px 15px; border-radius: 4px; font-weight: bold; }
            .summary-errors { background-color: var(--vscode-inputValidation-errorBackground); 
                             color: var(--vscode-inputValidation-errorForeground); }
            .summary-warnings { background-color: var(--vscode-inputValidation-warningBackground); 
                              color: var(--vscode-inputValidation-warningForeground); }
            .file-info { color: var(--vscode-descriptionForeground); font-size: 0.9em; }
            .error-list { display: flex; flex-direction: column; gap: 10px; }
            .error-item { padding: 15px; border-left: 4px solid; border-radius: 4px; 
                         background-color: var(--vscode-input-background); cursor: pointer; 
                         transition: background-color 0.2s; }
            .error-item:hover { background-color: var(--vscode-list-hoverBackground); }
            .error-error { border-color: var(--vscode-inputValidation-errorBorder); }
            .error-warning { border-color: var(--vscode-inputValidation-warningBorder); }
            .error-info { border-color: var(--vscode-inputValidation-infoBorder); }
            .error-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
            .error-location { color: var(--vscode-descriptionForeground); font-size: 0.9em; }
            .error-message { margin-bottom: 8px; line-height: 1.4; }
            .error-suggestion { color: var(--vscode-inputValidation-infoForeground); font-style: italic; margin-bottom: 8px; }
            button { padding: 6px 12px; border: 1px solid var(--vscode-button-border); 
                    background-color: var(--vscode-button-background); color: var(--vscode-button-foreground); 
                    border-radius: 2px; cursor: pointer; font-size: 0.8em; }
            button:hover { background-color: var(--vscode-button-hoverBackground); }
            .actions-bar { position: sticky; top: 0; background-color: var(--vscode-editor-background); 
                         padding: 15px 0; border-bottom: 1px solid var(--vscode-panel-border); 
                         margin-bottom: 20px; display: flex; gap: 10px; }
        </style>
    </head>
    <body>
        <div class="header">
            <h2>Résultats de Validation XML</h2>
            <div class="file-info">Fichier: ${params.xmlUri}</div>
            <div class="summary">
                <div class="summary-item summary-errors">${params.errorCount} Erreur(s)</div>
                <div class="summary-item summary-warnings">${params.warningCount} Avertissement(s)</div>
            </div>
        </div>
        
        <div class="actions-bar">
            <button onclick="validateAgain()">🔄 Revalider</button>
            <button onclick="copyResults()">📋 Copier le rapport</button>
        </div>
        
        <div class="error-list">
            ${params.errors.map(error => `
                <div class="error-item error-${error.severity}" onclick="showError(${error.line})">
                    <div class="error-header">
                        <strong>${error.code || error.severity}</strong>
                        <span class="error-location">Ligne ${error.line}, Colonne ${error.column}</span>
                    </div>
                    <div class="error-message">${escapeHtml(error.message)}</div>
                    ${error.suggestion ? `<div class="error-suggestion">💡 ${escapeHtml(error.suggestion)}</div>` : ''}
                </div>
            `).join('')}
        </div>
        
        <script>
            const vscode = acquireVsCodeApi();
            
            function showError(line) {
                vscode.postMessage({
                    command: 'showError',
                    line: line - 1,
                    message: 'Erreur ligne ' + line
                });
            }
            
            function validateAgain() {
                vscode.postMessage({ command: 'validateAgain' });
            }
            
            function copyResults() {
                const results = ${JSON.stringify(params.errors)};
                const text = results.map(error => 
                    'Ligne ' + error.line + ': [' + error.severity + '] ' + error.message
                ).join('\\n');
                navigator.clipboard.writeText(text);
            }
        </script>
    </body>
    </html>`;
}

async function showErrorInEditor(line: number, message: string) {
    const activeEditor = vscode.window.activeTextEditor;
    if (activeEditor) {
        const position = new vscode.Position(line, 0);
        const range = new vscode.Range(position, position);
        activeEditor.revealRange(range, vscode.TextEditorRevealType.InCenter);
        activeEditor.selection = new vscode.Selection(position, position);
    }
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}