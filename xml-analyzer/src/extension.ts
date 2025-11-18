import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';
import * as path from 'path';

interface ValidationResponse {
    success: boolean;
    diagnostics: vscode.Diagnostic[];
    errors: any[];
    fileSize: number;
    validationTime: number;
    errorCount: number;
    warningCount: number;
    summary: string;
}

// Collection de diagnostics
const diagnosticCollection = vscode.languages.createDiagnosticCollection('xml');

// Variable globale pour le client (accessible dans toutes les commandes)
let client: LanguageClient;

export function activate(context: vscode.ExtensionContext) {
    console.log('🚀 Activation de l\'extension XML Validator');

    // ========================================
    // 📡 INITIALISATION DU CLIENT LSP
    // ========================================
    
    // Chemin vers votre JAR du serveur LSP
    const serverJarPath = context.asAbsolutePath(
        path.join('server', 'xml-lsp-server.jar')
    );

    const fs = require('fs');
    if (!fs.existsSync(serverJarPath)) {
        vscode.window.showErrorMessage(`❌ Fichier JAR introuvable: ${serverJarPath}`);
        console.error('❌ JAR non trouvé à:', serverJarPath);
        return;
    }

    console.log('✅ JAR trouvé:', serverJarPath);

    // Configuration du serveur
    const serverOptions: ServerOptions = {
        run: {
            command: 'java',
            args: ['-jar', serverJarPath],
            transport: TransportKind.stdio
        },
        debug: {
            command: 'java',
            args: [
                '-jar',
                serverJarPath
            ],
            transport: TransportKind.stdio
        }
    };

    // Options du client
    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'xml' }],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.xml')
        }
    };

    // Créer le client
    client = new LanguageClient(
        'xmlLanguageServer',
        'XML Language Server',
        serverOptions,
        clientOptions
    );

    // Démarrer le client
    client.start();

    console.log('✅ Client LSP démarré');

    // ========================================
    // 🎯 ENREGISTREMENT DES COMMANDES
    // ========================================

    // Commande 1 : Valider le fichier actuel
    const validateCurrentCommand = vscode.commands.registerCommand(
        'xml.validateCurrent',
        async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) {
                vscode.window.showWarningMessage('Aucun fichier ouvert');
                return;
            }

            if (!editor.document.uri.fsPath.endsWith('.xml')) {
                vscode.window.showWarningMessage('Le fichier actuel n\'est pas un fichier XML');
                return;
            }

            // ✅ RÉCUPÉRATION DE L'URL : editor.document.uri
            await validateXmlFile(client, editor.document.uri);
        }
    );

    // Commande 2 : Valider avec XSD sélectionné
    const validateWithSchemaCommand = vscode.commands.registerCommand(
        'xml.validateWithSchema',
        async () => {
            // 📁 Sélectionner le fichier XML
            const xmlFiles = await vscode.window.showOpenDialog({
                canSelectMany: false,
                openLabel: 'Sélectionner le fichier XML',
                filters: { 'XML Files': ['xml'] },
                title: 'Sélectionner le fichier XML à valider'
            });

            if (!xmlFiles || xmlFiles.length === 0) {
                return;
            }

            // ✅ RÉCUPÉRATION DE L'URL XML : xmlFiles[0]
            const xmlUri = xmlFiles[0];
            console.log('📄 XML sélectionné:', xmlUri.toString());

            // Demander si on veut un XSD
            const useXsd = await vscode.window.showQuickPick(
                ['Oui, sélectionner un XSD', 'Non, valider sans schéma'],
                { placeHolder: 'Voulez-vous valider avec un schéma XSD ?' }
            );

            if (!useXsd) {
                return;
            }

            let xsdUri: vscode.Uri | undefined;

            if (useXsd === 'Oui, sélectionner un XSD') {
                const xsdFiles = await vscode.window.showOpenDialog({
                    canSelectMany: false,
                    openLabel: 'Sélectionner le schéma XSD',
                    filters: { 'XSD Files': ['xsd'], 'All Files': ['*'] },
                    title: 'Sélectionner le fichier XSD'
                });

                if (!xsdFiles || xsdFiles.length === 0) {
                    vscode.window.showWarningMessage('Aucun XSD sélectionné');
                    return;
                }

                // ✅ RÉCUPÉRATION DE L'URL XSD : xsdFiles[0]
                xsdUri = xsdFiles[0];
                console.log('📄 XSD sélectionné:', xsdUri.toString());
            }

            await validateXmlFile(client, xmlUri, xsdUri);
        }
    );

    // Commande 3 : Validation rapide
    const validateQuickCommand = vscode.commands.registerCommand(
        'xml.validateQuick',
        async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor || !editor.document.uri.fsPath.endsWith('.xml')) {
                vscode.window.showWarningMessage('Aucun fichier XML ouvert');
                return;
            }

            // ✅ RÉCUPÉRATION DE L'URL XML
            const xmlUri = editor.document.uri;
            
            // Chercher un XSD avec le même nom
            const xsdPath = xmlUri.fsPath.replace('.xml', '.xsd');
            let xsdUri: vscode.Uri | undefined;
            
            try {
                await vscode.workspace.fs.stat(vscode.Uri.file(xsdPath));
                // ✅ RÉCUPÉRATION DE L'URL XSD (si trouvé)
                xsdUri = vscode.Uri.file(xsdPath);
                vscode.window.showInformationMessage(`XSD trouvé : ${xsdPath}`);
            } catch {
                vscode.window.showInformationMessage('Validation sans schéma');
            }

            await validateXmlFile(client, xmlUri, xsdUri);
        }
    );

    // Enregistrer tout
    context.subscriptions.push(
        validateCurrentCommand,
        validateWithSchemaCommand,
        validateQuickCommand,
        diagnosticCollection,
        client  // Important : disposer le client à la désactivation
    );
}

// ========================================
// 📤 FONCTION D'ENVOI AU SERVEUR
// ========================================
async function validateXmlFile(
    client: LanguageClient,
    xmlUri: vscode.Uri,
    xsdUri?: vscode.Uri
) {
    try {
        console.log('📤 Envoi de la requête au serveur LSP...');
        console.log('   XML URI:', xmlUri.toString());
        console.log('   XSD URI:', xsdUri?.toString() || 'null');

        // 🎯 ENVOI DES URLS AU SERVEUR
        const response = await client.sendRequest<ValidationResponse>(
            'xml/validateFiles',
            {
                xmlUri: xmlUri.toString(),    // ← Conversion Uri → String
                xsdUri: xsdUri?.toString()    // ← Conversion Uri → String (ou undefined)
            }
        );

        console.log('✅ Réponse reçue:', response);
        console.log(`📊 ${response.errorCount} erreurs, ${response.warningCount} warnings`);

        // Afficher les diagnostics
        if (response.diagnostics && response.diagnostics.length > 0) {
            const vscodeDiagnostics = response.diagnostics.map(d => 
                convertToVsCodeDiagnostic(d)
            );
            
            diagnosticCollection.set(xmlUri, vscodeDiagnostics);
            
            vscode.window.showInformationMessage(
                `Validation terminée: ${response.errorCount} erreurs trouvées`
            );
        } else {
            diagnosticCollection.clear();
            vscode.window.showInformationMessage('✅ Aucune erreur trouvée !');
        }

        // Afficher le résumé
        const outputChannel = vscode.window.createOutputChannel('XML Validation');
        outputChannel.appendLine(`\n=== Validation de ${xmlUri.fsPath} ===`);
        outputChannel.appendLine(`Taille: ${response.fileSize} bytes`);
        outputChannel.appendLine(`Temps: ${response.validationTime} ms`);
        outputChannel.appendLine(`Résultat: ${response.summary}`);
        outputChannel.show();

    } catch (error) {
        console.error('❌ Erreur lors de la validation:', error);
        vscode.window.showErrorMessage(`Erreur de validation: ${error}`);
    }
}

function convertToVsCodeDiagnostic(lspDiag: any): vscode.Diagnostic {
    const range = new vscode.Range(
        lspDiag.range.start.line,
        lspDiag.range.start.character,
        lspDiag.range.end.line,
        lspDiag.range.end.character
    );

    const severity = mapSeverity(lspDiag.severity);
    const diagnostic = new vscode.Diagnostic(range, lspDiag.message, severity);
    diagnostic.source = lspDiag.source || 'xml-validator';
    diagnostic.code = lspDiag.code;
    return diagnostic;
}

function mapSeverity(lspSeverity: number): vscode.DiagnosticSeverity {
    switch (lspSeverity) {
        case 1: return vscode.DiagnosticSeverity.Error;
        case 2: return vscode.DiagnosticSeverity.Warning;
        case 3: return vscode.DiagnosticSeverity.Information;
        case 4: return vscode.DiagnosticSeverity.Hint;
        default: return vscode.DiagnosticSeverity.Error;
    }
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}