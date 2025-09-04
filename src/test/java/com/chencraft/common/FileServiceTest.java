package com.chencraft.common;

import com.chencraft.common.service.file.FileService;
import com.chencraft.common.service.file.LocalFileService;
import com.chencraft.model.FileUpload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest(classes = {LocalFileService.class})
public class FileServiceTest {
    private static final String TEST_FILE = "FileServiceTest.txt";
    private static final String TEST_FILE_PATH = "src/test/resources/public/" + TEST_FILE;
    private static final String TEST_CONTENT = "Test content for valid file";

    @Autowired
    private FileService fileService;

    @BeforeAll
    public static void init() throws IOException {
        Path testFile = Paths.get(TEST_FILE_PATH);
        if (!Files.exists(testFile)) {
            Files.createDirectories(testFile.getParent());
            Files.writeString(testFile, TEST_CONTENT);
        }
        Assertions.assertTrue(Files.exists(testFile));
    }

    @Test
    public void validFileServed() {
        ResponseEntity<Resource> response = fileService.downloadFile(FileUpload.Type.PUBLIC, TEST_FILE);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(TEST_FILE, response.getBody().getFilename());
    }

    @Test
    public void invalidFileServed() {
        ResponseEntity<Resource> response = fileService.downloadFile(FileUpload.Type.PUBLIC, "invalid-file.txt");
        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_FILE_PATH));
    }
}