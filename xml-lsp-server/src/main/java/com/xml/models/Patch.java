package com.xml.models;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Représente une modification atomique sur le fichier XML.
 * 
 * <h2>Terminologie des offsets:</h2>
 * <ul>
 *   <li><b>originalStartOffset/originalEndOffset</b>: Position dans le fichier ORIGINAL sur disque.
 *       Utilisé par FileSaver pour savoir quelle zone du fichier original doit être remplacée.</li>
 *   <li><b>globalStartOffset/globalEndOffset</b>: Alias pour compatibilité. Dans l'implémentation actuelle,
 *       ces valeurs sont identiques aux offsets originaux.</li>
 * </ul>
 * 
 * <h2>Longueurs:</h2>
 * <ul>
 *   <li><b>originalLength</b>: Nombre de bytes dans le fichier original à remplacer
 *       (= originalEndOffset - originalStartOffset)</li>
 *   <li><b>newLength</b>: Nombre de bytes UTF-8 du texte de remplacement</li>
 * </ul>
 */
public class Patch implements Comparable<Patch> {
    
    // Offsets dans le fichier ORIGINAL
    private final long originalStartOffset;
    private final long originalEndOffset;
    
    private final String replacementText;
    private final PatchType type;
    private final String fragmentId;
    
    // Longueurs calculées (cached)
    private final int originalLength;
    private final int newLength;

    /**
     * Constructeur principal.
     * 
     * @param originalStartOffset Offset de début dans le fichier ORIGINAL (inclusif)
     * @param originalEndOffset Offset de fin dans le fichier ORIGINAL (exclusif)
     * @param replacementText Texte de remplacement (peut être vide pour DELETE, ou plus long/court)
     * @param type Type de patch (INSERT, DELETE, REPLACE)
     * @param fragmentId ID du fragment concerné (peut être null)
     */
    public Patch(long originalStartOffset, long originalEndOffset, String replacementText, PatchType type, String fragmentId) {
        // Validation: pour INSERT, start == end est valide
        if (originalStartOffset < 0) {
            throw new IllegalArgumentException("originalStartOffset doit être >= 0: " + originalStartOffset);
        }
        if (originalEndOffset < originalStartOffset) {
            throw new IllegalArgumentException("Offsets invalides: [" + originalStartOffset + ", " + originalEndOffset + ")");
        }
        
        this.originalStartOffset = originalStartOffset;
        this.originalEndOffset = originalEndOffset;
        this.replacementText = replacementText != null ? replacementText : "";
        this.type = type;
        this.fragmentId = fragmentId;
        
        // Calcul des longueurs
        this.originalLength = (int)(originalEndOffset - originalStartOffset);
        this.newLength = this.replacementText.getBytes(StandardCharsets.UTF_8).length;
    }
    
    // === OFFSETS ORIGINAUX (pour FileSaver) ===
    
    /**
     * @return Offset de début dans le fichier ORIGINAL (inclusif)
     */
    public long getOriginalStartOffset() {
        return originalStartOffset;
    }
    
    /**
     * @return Offset de fin dans le fichier ORIGINAL (exclusif).
     *         C'est cette valeur que FileSaver doit utiliser pour savoir combien de bytes sauter.
     */
    public long getOriginalEndOffset() {
        return originalEndOffset;
    }
    
    // === ALIAS POUR COMPATIBILITE ===
    
    /**
     * @return Alias pour getOriginalStartOffset() - maintenu pour compatibilité
     */
    public long getGlobalStartOffset() {
        return originalStartOffset;
    }

    /**
     * @return Alias pour getOriginalEndOffset() - maintenu pour compatibilité
     */
    public long getGlobalEndOffset() {
        return originalEndOffset;
    }
    
    // === LONGUEURS ===
    
    /**
     * @return Nombre de bytes à remplacer dans le fichier original
     */
    public int getOriginalLength() {
        return originalLength;
    }
    
    /**
     * @return Nombre de bytes UTF-8 du texte de remplacement
     */
    public int getNewLength() {
        return newLength;
    }
    
    /**
     * @return Différence de taille (newLength - originalLength).
     *         Positif si le patch agrandit le document, négatif s'il le réduit.
     */
    public int getLengthDelta() {
        return newLength - originalLength;
    }

    // === ACCESSEURS STANDARD ===
    
    public String getReplacementText() {
        return replacementText;
    }

    public PatchType getType() {
        return type;
    }

    public String getFragmentId() {
        return fragmentId;
    }

    /**
     * Trie les patchs par offset de début ORIGINAL croissant.
     * C'est l'ordre requis pour appliquer les patches au fichier.
     */
    @Override
    public int compareTo(Patch other) {
        return Long.compare(this.originalStartOffset, other.originalStartOffset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Patch patch = (Patch) o;
        return originalStartOffset == patch.originalStartOffset &&
                originalEndOffset == patch.originalEndOffset &&
                Objects.equals(replacementText, patch.replacementText) &&
                type == patch.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalStartOffset, originalEndOffset, replacementText, type);
    }

    @Override
    public String toString() {
        return "Patch{" +
                "orig=[" + originalStartOffset + ", " + originalEndOffset + ")" +
                ", type=" + type +
                ", origLen=" + originalLength +
                ", newLen=" + newLength +
                ", text='" + (replacementText.length() > 20 ? replacementText.substring(0, 20) + "..." : replacementText) + '\'' +
                ", frag='" + fragmentId + '\'' +
                '}';
    }
}
