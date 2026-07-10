package com.example.fonos_group13.data.library;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fonos_group13.model.BookChapter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FinalDataArchitectureTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void failedReplacementPreservesExistingDownload() throws Exception {
        DownloadedAudioStore store = new DownloadedAudioStore(temporaryFolder.getRoot());
        File target = store.audioFile("book", BookChapter.LEGACY_CHAPTER_ID);
        byte[] original = "original audio".getBytes(StandardCharsets.UTF_8);
        Files.write(target.toPath(), original);
        File missingTemp = new File(temporaryFolder.getRoot(), "missing.tmp");

        assertFalse(store.replaceFromTemp(missingTemp, target));
        assertArrayEquals(original, Files.readAllBytes(target.toPath()));
    }

    @Test
    public void completionPolicyUsesNinetyFivePercentBoundary() {
        assertFalse(ProgressCompletionPolicy.isCompleted(949, 1000));
        assertTrue(ProgressCompletionPolicy.isCompleted(950, 1000));
    }

    @Test
    public void activitiesDoNotConstructRepositoriesOrImportFirebaseImplementations() throws Exception {
        Path directory = Paths.get("src/main/java/com/example/fonos_group13");
        try (java.util.stream.Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.getFileName().toString().endsWith("Activity.java"))
                    .forEach(path -> {
                        try {
                            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            assertFalse(path + " constructs a repository", source.matches(
                                    "(?s).*new\\s+[A-Za-z]+Repository\\s*\\(.*"
                            ));
                            assertFalse(path + " imports FirebaseAuth", source.contains(
                                    "import com.google.firebase.auth.FirebaseAuth"
                            ));
                            assertFalse(path + " imports Firestore listeners", source.contains(
                                    "import com.google.firebase.firestore.ListenerRegistration"
                            ));
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    });
        }
    }
}
