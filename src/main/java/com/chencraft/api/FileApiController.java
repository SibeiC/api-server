package com.chencraft.api;

import com.chencraft.common.service.file.FileService;
import com.chencraft.model.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public file download controller implementing FileApi. Serves files from the PUBLIC bucket.
 * Uses FileService for actual storage access.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T12:22:59.887751009Z[Etc/UTC]")
@RestController
public class FileApiController implements FileApi {

    private final FileService fileService;

    /**
     * Constructs FileApiController.
     *
     * @param fileService file storage service abstraction
     */
    @Autowired
    public FileApiController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Downloads a public file by filename.
     *
     * @param filename object key to fetch from PUBLIC storage
     * @return HTTP 200 with resource body or appropriate error from FileService
     */
    @Override
    public ResponseEntity<Resource> file(String filename) {
        return fileService.downloadFile(FileUpload.Type.PUBLIC, filename);
    }
}
