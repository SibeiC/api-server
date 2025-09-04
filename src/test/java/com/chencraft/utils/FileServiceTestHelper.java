package com.chencraft.utils;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileServiceTestHelper {
    public static void createDirectory(String dirPath) {
        try {
            Path dir = Paths.get(dirPath);
            Files.createDirectories(dir);
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    public static void createFile(String filePath) {
        createFile(filePath, "Test content for valid file");
    }

    public static void createFile(String filePath, String fileContent) {
        try {
            Path testFile = Paths.get(filePath);
            if (!Files.exists(testFile)) {
                Files.createDirectories(testFile.getParent());
                Files.writeString(testFile, fileContent);
            }
            Assertions.assertTrue(Files.exists(testFile));
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    public static void deleteDirectory(String dirPath) {
        Path dir = Paths.get(dirPath);
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder()) // children before parent
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                         } catch (IOException e) {
                             Assertions.fail(e);
                         }
                     });
            } catch (IOException e) {
                Assertions.fail(e);
            }
        }
    }

    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }
}
