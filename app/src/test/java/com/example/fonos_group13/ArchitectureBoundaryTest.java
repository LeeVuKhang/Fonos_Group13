package com.example.fonos_group13;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ArchitectureBoundaryTest {
    @Test
    public void modelsDoNotImportFirebaseOrAndroidFrameworks() throws Exception {
        Path modelDirectory = Paths.get("src/main/java/com/example/fonos_group13/model");
        try (java.util.stream.Stream<Path> paths = Files.list(modelDirectory)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            String source = readFile(path);
                            assertFalse(path + " imports Firebase", source.contains("import com.google.firebase"));
                            assertFalse(path + " imports Android", source.contains("import android."));
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    });
        }
    }

    @Test
    public void repositoryContractsExistAsInterfaces() throws Exception {
        List<String> contractNames = Arrays.asList(
                "AuthRepository",
                "CatalogRepository",
                "SavedBooksRepository",
                "ProgressRepository",
                "AudioDownloadRepository",
                "CreatorCommandRepository",
                "CreatorUploadsRepository"
        );

        for (String contractName : contractNames) {
            Path path = Paths.get(
                    "src/main/java/com/example/fonos_group13/data/repository/" + contractName + ".java"
            );
            assertTrue(contractName + " contract is missing", Files.exists(path));
            assertTrue(readFile(path).contains("public interface " + contractName));
        }
    }

    private String readFile(String path) throws Exception {
        return readFile(Paths.get(path));
    }

    private String readFile(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
