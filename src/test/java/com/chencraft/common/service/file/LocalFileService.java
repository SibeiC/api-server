package com.chencraft.common.service.file;

import com.chencraft.api.ApiException;
import com.chencraft.model.FileUpload;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

@Slf4j
@Primary
@Component
public class LocalFileService implements FileService {
    private final Path basePath;

    public LocalFileService(@Value("${app.storage.location}") String storageLocation) throws IOException {
        storageLocation = storageLocation.endsWith("/") ? storageLocation : storageLocation + "/";
        this.basePath = Paths.get(storageLocation);

        if (!this.basePath.toFile().exists()) {
            log.info("Creating directory {}", this.basePath.toAbsolutePath());
            if (!this.basePath.toFile().mkdirs()) {
                throw new IOException("Could not create directory " + this.basePath.toAbsolutePath());
            }
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
            Files.setPosixFilePermissions(this.basePath.toFile().toPath(), permissions);
        }
    }

    @Override
    public void uploadFile(FileUpload.Type destination, String filename, String contentType, byte[] content) {
        File file = this.basePath.resolve(destination.toPrefix()).resolve(filename).toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error saving file: " + filename, e);
        }
    }

    @Override
    public ResponseEntity<Resource> downloadFile(FileUpload.Type destination, @NotNull String filename) {
        Path filePath = this.basePath.resolve(destination.toPrefix()).resolve(filename);

        if (!filePath.toFile().exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not determine file type.");
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                             .contentType(MediaType.parseMediaType(contentType))
                             .body(new FileSystemResource(filePath.toFile()));
    }
}