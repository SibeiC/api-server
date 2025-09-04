package com.chencraft.api.secure;

import com.chencraft.api.ApiException;
import com.chencraft.common.service.file.FileService;
import com.chencraft.model.FileUpload;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T12:22:59.887751009Z[Etc/UTC]")
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureFileApiController implements SecureFileApi {

    private final FileService fileService;

    @Autowired
    public SecureFileApiController(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public ResponseEntity<Resource> secureFile(String filename) {
        return fileService.downloadFile(FileUpload.Type.PRIVATE, filename);
    }

    @Override
    public ResponseEntity<Void> uploadFile(FileUpload request) {
        String filename = request.getFile().getOriginalFilename();
        String contentType = request.getFile().getContentType();
        byte[] content;
        try {
            content = request.getFile().getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read in file: " + filename, e);
        }
        fileService.uploadFile(request.getDestination(), filename, contentType, content);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
