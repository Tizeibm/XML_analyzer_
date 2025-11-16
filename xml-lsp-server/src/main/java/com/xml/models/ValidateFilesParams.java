package com.xml;

/**
 * Paramètres pour la requête xml/validateFiles
 * Contient les URIs du fichier XML et optionnellement du schéma XSD.
 */
public class ValidateFilesParams {
    public String xmlUri;   // file:///path/to/file.xml
    public String xsdUri;   // file:///path/to/schema.xsd ou null

    public ValidateFilesParams(String xmlfile, String xsdfile) {
    }
}
