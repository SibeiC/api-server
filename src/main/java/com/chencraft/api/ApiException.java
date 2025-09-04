package com.chencraft.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-22T12:16:37.352130473Z[Etc/UTC]")
@Getter
public class ApiException extends RuntimeException {
    protected final HttpStatus code;

    public ApiException(HttpStatus code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }

    public ApiException(HttpStatus code, String msg) {
        super(msg);
        this.code = code;
    }
}
