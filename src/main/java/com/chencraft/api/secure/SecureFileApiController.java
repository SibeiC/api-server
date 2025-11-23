package com.chencraft.api.secure;

import com.chencraft.api.ApiException;
import com.chencraft.common.service.file.FileService;
import com.chencraft.common.service.file.FileTokenService;
import com.chencraft.model.FileUpload;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * Secure file API controller implementing SecureFileApi. Requires mTLS as configured.
 * Handles uploads and private file downloads via FileService.
 */
@Slf4j
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T12:22:59.887751009Z[Etc/UTC]")
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureFileApiController implements SecureFileApi {

    private final FileService fileService;
    private final FileTokenService fileTokenService;

    /**
     * Constructs SecureFileApiController.
     *
     * @param fileService file storage service abstraction
     */
    @Autowired
    public SecureFileApiController(FileService fileService, FileTokenService fileTokenService) {
        this.fileService = fileService;
        this.fileTokenService = fileTokenService;
    }

    /**
     * Downloads a private file by filename.
     *
     * @param filename object key from the PRIVATE storage
     * @return resource stream or error from FileService
     */
    @Override
    public ResponseEntity<@NonNull Resource> secureFile(String filename) {
        return fileService.downloadFile(FileUpload.Type.PRIVATE, filename);
    }

    /**
     * Uploads a file to the configured destination in PRIVATE storage.
     *
     * @param request multipart request binding including destination and file
     * @return HTTP 200 when uploaded
     * @throws com.chencraft.api.ApiException if file bytes cannot be read
     */
    @Override
    public ResponseEntity<?> uploadFile(FileUpload request) {
        String filename = request.getFile().getOriginalFilename();
        String contentType = request.getFile().getContentType();
        byte[] content;
        try {
            content = request.getFile().getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read in file: " + filename, e);
        }

        fileService.uploadFile(request.getDestination(), filename, contentType, content);
        if (request.getDestination() == FileUpload.Type.SHARE) {
            String accessUrl = fileTokenService.generateAccessToken(filename);
            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of(
                                         "message", "File uploaded successfully for one-time sharing.",
                                         "url", accessUrl
                                 ));
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<?> deleteFile(String filename, FileUpload.Type namespace) {
        fileService.deleteFile(namespace, filename);
        if (namespace == FileUpload.Type.SHARE) {
            fileTokenService.revokeAccessToken(filename);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
