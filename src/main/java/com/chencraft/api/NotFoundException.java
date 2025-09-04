package com.chencraft.api;

import org.springframework.http.HttpStatus;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
public class NotFoundException extends ApiException {
    public NotFoundException(String contentName) {
        super(HttpStatus.NOT_FOUND, String.format("'%s' was not found.", contentName));
    }
}
