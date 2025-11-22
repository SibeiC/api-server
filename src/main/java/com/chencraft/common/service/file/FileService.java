package com.chencraft.common.service.file;

import com.chencraft.model.FileUpload;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FileService {
    void uploadFile(FileUpload.Type destination, String filename, String contentType, byte[] content);

    ResponseEntity<@NonNull Resource> downloadFile(FileUpload.Type destination, @Nonnull String filename);

    void deleteFile(FileUpload.Type destination, @Nonnull String filename);
}
