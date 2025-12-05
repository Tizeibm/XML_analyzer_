package com.xml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xml.models.Patch;
import com.xml.models.PatchType;
import com.xml.services.FileSaver;
import com.xml.services.PatchManager;

public class PatchingWorkflowTest {

    private Path tempDir;
    private PatchManager patchManager;
    private FileSaver fileSaver;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("patch-test");
        patchManager = new PatchManager(tempDir);
        fileSaver = new FileSaver();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup
        patchManager.clearAll();
        Files.walk(tempDir)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);
    }

    @Test
    void testPatchAdditionAndSorting() {
        Patch p1 = new Patch(100, 110, "replacement1", PatchType.REPLACE, "f1");
        Patch p2 = new Patch(50, 60, "replacement2", PatchType.REPLACE, "f2");
        Patch p3 = new Patch(150, 150, "insertion", PatchType.INSERT, "f3");

        patchManager.addPatch(p1);
        patchManager.addPatch(p2);
        patchManager.addPatch(p3);

        List<Patch> sorted = patchManager.getAllPatchesSorted();
        assertEquals(3, sorted.size());
        assertEquals(p2, sorted.get(0)); // 50
        assertEquals(p1, sorted.get(1)); // 100
        assertEquals(p3, sorted.get(2)); // 150
    }

    @Test
    void testConflictResolution() {
        // Patch original: [100, 120)
        Patch p1 = new Patch(100, 120, "original", PatchType.REPLACE, "f1");
        patchManager.addPatch(p1);

        // Patch conflictuel: [110, 130) -> chevauche la fin de p1
        Patch p2 = new Patch(110, 130, "overlap", PatchType.REPLACE, "f1");
        patchManager.addPatch(p2);

        List<Patch> sorted = patchManager.getAllPatchesSorted();
        assertEquals(1, sorted.size());
        assertEquals(p2, sorted.get(0)); // Le dernier gagne
    }

    @Test
    void testPersistence() {
        Patch p1 = new Patch(100, 110, "persistent", PatchType.REPLACE, "f1");
        patchManager.addPatch(p1);

        // Simuler un redémarrage
        PatchManager newManager = new PatchManager(tempDir);
        List<Patch> loaded = newManager.getAllPatchesSorted();

        assertEquals(1, loaded.size());
        assertEquals(p1, loaded.get(0));
    }

    @Test
    void testFileSaving() throws IOException {
        // Créer un fichier XML dummy
        Path xmlPath = tempDir.resolve("test.xml");
        String content = "<root><item>Original</item><item>ToDelete</item></root>";
        Files.writeString(xmlPath, content);

        // Patch 1: Remplacer "Original" par "Patched"
        // <root><item>Original</item>...
        // 01234567890123456789012
        // "Original" est à l'offset 12 (longueur 8)
        Patch p1 = new Patch(12, 20, "Patched", PatchType.REPLACE, "f1");
        patchManager.addPatch(p1);

        // Patch 2: Supprimer le deuxième item
        // <item>ToDelete</item> commence après </item> (offset 27)
        // <item>ToDelete</item> -> offset 27 à 48 (longueur 21)
        Patch p2 = new Patch(27, 48, "", PatchType.DELETE, "f2");
        patchManager.addPatch(p2);

        // Sauvegarder
        fileSaver.saveWithPatches(xmlPath.toFile(), xmlPath.toFile(), null, patchManager);

        // Vérifier le contenu
        String newContent = Files.readString(xmlPath);
        String expected = "<root><item>Patched</item></root>";
        assertEquals(expected, newContent);
    }
    
    /**
     * TEST CRITIQUE: Vérifie que les patches avec longueurs différentes fonctionnent.
     * C'est le bug principal signalé avec VS Code.
     */
    @Test
    void testFileSavingWithExpandingPatch() throws IOException {
        // Original: "<root>AB</root>" (15 bytes)
        Path xmlPath = tempDir.resolve("expand.xml");
        String content = "<root>AB</root>";
        Files.writeString(xmlPath, content);
        
        // Patch: Remplacer "AB" (2 bytes, offset 6-8) par "EXPANDED_TEXT" (13 bytes)
        // Cette situation provoquait un "trou" avant le fix
        Patch p = new Patch(6, 8, "EXPANDED_TEXT", PatchType.REPLACE, "f1");
        patchManager.addPatch(p);
        
        fileSaver.saveWithPatches(xmlPath.toFile(), xmlPath.toFile(), null, patchManager);
        
        String newContent = Files.readString(xmlPath);
        String expected = "<root>EXPANDED_TEXT</root>";
        assertEquals(expected, newContent);
        assertEquals(26, newContent.length()); // 15 - 2 + 13 = 26
    }
    
    @Test
    void testFileSavingWithShrinkingPatch() throws IOException {
        // Original: "<root>LONG_ORIGINAL_TEXT</root>" (31 bytes)
        Path xmlPath = tempDir.resolve("shrink.xml");
        String content = "<root>LONG_ORIGINAL_TEXT</root>";
        Files.writeString(xmlPath, content);
        
        // Patch: Remplacer "LONG_ORIGINAL_TEXT" (18 bytes, offset 6-24) par "XY" (2 bytes)
        Patch p = new Patch(6, 24, "XY", PatchType.REPLACE, "f1");
        patchManager.addPatch(p);
        
        fileSaver.saveWithPatches(xmlPath.toFile(), xmlPath.toFile(), null, patchManager);
        
        String newContent = Files.readString(xmlPath);
        String expected = "<root>XY</root>";
        assertEquals(expected, newContent);
        assertEquals(15, newContent.length()); // 31 - 18 + 2 = 15
    }
    
    @Test
    void testMultipleLengthChangingPatches() throws IOException {
        // Original: "<a>111</a><b>222</b><c>333</c>" (30 bytes)
        Path xmlPath = tempDir.resolve("multi.xml");
        String content = "<a>111</a><b>222</b><c>333</c>";
        Files.writeString(xmlPath, content);
        
        // Patch 1: "111" (offset 3-6) -> "ALPHA" (5 bytes) - expand
        Patch p1 = new Patch(3, 6, "ALPHA", PatchType.REPLACE, "f1");
        // Patch 2: "222" (offset 13-16) -> "B" (1 byte) - shrink
        Patch p2 = new Patch(13, 16, "B", PatchType.REPLACE, "f2");
        // Patch 3: "333" (offset 23-26) -> "GAMMA" (5 bytes) - expand
        Patch p3 = new Patch(23, 26, "GAMMA", PatchType.REPLACE, "f3");
        
        patchManager.addPatch(p1);
        patchManager.addPatch(p2);
        patchManager.addPatch(p3);
        
        fileSaver.saveWithPatches(xmlPath.toFile(), xmlPath.toFile(), null, patchManager);
        
        String newContent = Files.readString(xmlPath);
        String expected = "<a>ALPHA</a><b>B</b><c>GAMMA</c>";
        assertEquals(expected, newContent);
    }
    
    @Test
    void testPatchLengthMethods() {
        // Vérifier les nouvelles méthodes de longueur
        Patch insert = new Patch(10, 10, "INSERTED", PatchType.INSERT, "f1");
        assertEquals(0, insert.getOriginalLength());
        assertEquals(8, insert.getNewLength());
        assertEquals(8, insert.getLengthDelta()); // +8
        
        Patch delete = new Patch(10, 20, "", PatchType.DELETE, "f1");
        assertEquals(10, delete.getOriginalLength());
        assertEquals(0, delete.getNewLength());
        assertEquals(-10, delete.getLengthDelta()); // -10
        
        Patch replace = new Patch(10, 15, "REPLACEMENT", PatchType.REPLACE, "f1");
        assertEquals(5, replace.getOriginalLength());
        assertEquals(11, replace.getNewLength());
        assertEquals(6, replace.getLengthDelta()); // +6
    }
}

