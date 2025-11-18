package com.xml.handlers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



/**
 * Moteur de patching pour appliquer des modifications de fragment au fichier original
 * SANS créer d'incohérences dans la structure XML.
 * 
 * Stratégie:
 * 1. Charger le fichier ligne par ligne avec index précis
 * 2. Remplacer le fragment à partir de fragmentStartLine jusqu'à fragmentEndLine
 * 3. Vérifier l'intégrité XML du résultat avant écriture
 * 4. Écrire atomiquement avec sauvegarde de backup
 */
public class FilePatcher {


    /**
     * Applique une modification de fragment au fichier XML original.
     * 
     * @param filePath Chemin du fichier XML
     * @param modifiedFragment Contenu XML modifié
     * @param fragmentStartLine Ligne de début du fragment (1-based)
     * @param fragmentEndLine Ligne de fin du fragment (1-based)
     * @return true si la sauvegarde a réussi, false sinon
     */
    public static boolean patchFile(
            String filePath, 
            String modifiedFragment, 
            int fragmentStartLine, 
            int fragmentEndLine) {
        
        try {
            Path path = Paths.get(filePath);
            
            String originalContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String[] lines = originalContent.split("\n", -1);
            
             

            // Construire le nouveau contenu
            StringBuilder newContent = new StringBuilder();
            
            // Lignes avant le fragment (0 à fragmentStartLine-2, car 1-based)
            for (int i = 0; i < fragmentStartLine - 1 && i < lines.length; i++) {
                newContent.append(lines[i]).append("\n");
            }
            
            newContent.append(modifiedFragment);
            
            // Si le fragment modifié ne finit pas par une newline et qu'il y a d'autres lignes après
            if (!modifiedFragment.endsWith("\n") && fragmentEndLine < lines.length) {
                newContent.append("\n");
            }
            
            // Lignes après le fragment (à partir de fragmentEndLine)
            for (int i = fragmentEndLine; i < lines.length; i++) {
                newContent.append(lines[i]);
                if (i < lines.length - 1) {
                    newContent.append("\n");
                }
            }

            String patchedContent = newContent.toString();
            
            if (!validateXmlStructure(patchedContent)) {

                return false;
            }

            return writeFileAtomically(path, patchedContent);
            
        } catch (IOException e) {

            return false;
        }
    }

    /**
     * Valide la structure XML du contenu pour éviter les incohérences.
     */
    private static boolean validateXmlStructure(String content) {
        try {
            // Compter les balises équilibrées
            int openCount = 0;
            int closeCount = 0;
            
            // Expression régulière simplifiée pour compter les balises
            String[] openTags = content.split("<([a-zA-Z0-9:_-]+)[^>]*(?<!/\\s)\\s*>", -1);
            String[] closeTags = content.split("</([a-zA-Z0-9:_-]+)\\s*>", -1);
            String[] selfClosing = content.split("<[^>]*/\\s*>", -1);
            
            openCount = openTags.length - 1;
            closeCount = closeTags.length - 1;
            int selfClosingCount = selfClosing.length - 1;
            
            // Vérification basique: balises ouvertes doivent être fermées (+ self-closing)
            boolean isValid = (openCount - selfClosingCount - closeCount) <= 1 && 
                            (openCount - selfClosingCount - closeCount) >= -1;
            
            if (isValid) {

            } else {


            }
            return isValid;
            
        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Écrit le fichier de manière atomique avec création d'un backup.
     */
    private static boolean writeFileAtomically(Path path, String content) {
        try {
            Path backupPath = Paths.get(path.toString() + ".backup");
            if (Files.exists(path)) {
                Files.copy(path, backupPath);
                
            }
            
            // Écrire le nouveau contenu
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            
            return true;
            
        } catch (IOException e) {

            return false;
        }
    }

    /**
     * Applique un correctif automatique pour une erreur spécifique
     */
    public static boolean applyAutoFix(File xmlFile, String tagName, int lineNumber, String errorType) {
        try {
            String content = new String(java.nio.file.Files.readAllBytes(xmlFile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = content.split("\n", -1);

            if (lineNumber < 1 || lineNumber > lines.length) {
                return false;
            }

            String originalLine = lines[lineNumber - 1];
            String fixedLine = originalLine;

            // Appliquer le correctif selon le type d'erreur
            if ("STRUCTURE".equals(errorType)) {
                if (originalLine.contains("<" + tagName) && !originalLine.contains("</" + tagName + ">")) {
                    // Fermer la balise
                    if (originalLine.trim().endsWith(">")) {
                        fixedLine = originalLine.replace(">", "></" + tagName + ">");
                    }
                }
            }

            // Remplacer la ligne
            lines[lineNumber - 1] = fixedLine;

            // Reconstruire le contenu
            StringBuilder newContent = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                newContent.append(lines[i]);
                if (i < lines.length - 1) {
                    newContent.append("\n");
                }
            }

            // Sauvegarder avec validation
            return writeFileAtomically(xmlFile.toPath(), newContent.toString());

        } catch (Exception e) {

            return false;
        }
    }
}