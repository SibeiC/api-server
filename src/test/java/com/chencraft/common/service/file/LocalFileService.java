package com.chencraft.common.service.file;

import com.chencraft.api.NotFoundException;
import com.chencraft.model.FileUpload;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Primary
@Component
public class LocalFileService implements FileService {
    private final Path basePath;
    private final Map<String, FileData> storage = new ConcurrentHashMap<>();

    private record FileData(byte[] content, String contentType) {
    }

    public LocalFileService(@Value("${app.storage.location}") String storageLocation) {
        storageLocation = storageLocation.endsWith("/") ? storageLocation : storageLocation + "/";
        this.basePath = Paths.get(storageLocation);
        log.info("Initialized In-Memory LocalFileService. Writes are in-memory. Reads fall back to {}", this.basePath.toAbsolutePath());
    }

    @Override
    public void uploadFile(FileUpload.Type destination, String filename, String contentType, byte[] content) {
        String key = getMapKey(destination, filename);
        storage.put(key, new FileData(content, contentType));
        log.info("Uploaded file to memory: {}", key);
    }

    @Override
    public ResponseEntity<@NonNull Resource> downloadFile(FileUpload.Type destination, @NotNull String filename) {
        String key = getMapKey(destination, filename);
        FileData data = storage.get(key);

        if (data != null) {
            return ResponseEntity.ok()
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                                 .contentType(MediaType.parseMediaType(data.contentType))
                                 .body(new ByteArrayResource(data.content));
        }

        // Fallback to disk for pre-existing fixtures (read-only)
        Path filePath = this.basePath.resolve(destination.toPrefix()).resolve(filename);
        if (Files.exists(filePath)) {
            try {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                                     .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                                     .contentType(MediaType.parseMediaType(contentType))
                                     .body(new FileSystemResource(filePath.toFile()));
            } catch (IOException e) {
                log.error("Error reading file from disk: {}", filePath, e);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteFile(FileUpload.Type destination, @NotNull String filename) {
        String key = getMapKey(destination, filename);
        boolean removedFromMemory = storage.remove(key) != null;

        // Try to delete from disk if it exists, to support tests that check disk
        Path filePath = this.basePath.resolve(destination.toPrefix()).resolve(filename);
        boolean removedFromDisk = false;
        if (Files.exists(filePath)) {
            try {
                removedFromDisk = Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete file from disk: {}", filePath, e);
            }
        }

        if (!removedFromMemory && !removedFromDisk) {
            throw new NotFoundException(filename);
        }
    }

    private String getMapKey(FileUpload.Type destination, String filename) {
        return destination.name() + ":" + filename;
    }
}
