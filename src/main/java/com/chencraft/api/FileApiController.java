package com.chencraft.api;

import com.chencraft.common.service.file.FileService;
import com.chencraft.model.FileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T12:22:59.887751009Z[Etc/UTC]")
@RestController
public class FileApiController implements FileApi {

    private final FileService fileService;

    @Autowired
    public FileApiController(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public ResponseEntity<Resource> file(String filename) {
        return fileService.downloadFile(FileUpload.Type.PUBLIC, filename);
    }
}
