package com.chencraft.api.secure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.TLS;

@Validated
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public interface SecurePingApi {
    /**
     * Simple secured liveness endpoint to verify mTLS-protected connectivity.
     * Returns 200 when the request passes mTLS verification, otherwise the filter returns 401.
     */
    @Operation(summary = "Test mTLS connection", description = "Check mTLS verification is successful.", security = {@SecurityRequirement(name = "mTLS")}, tags = {TLS})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", ref = OK_RESPONSE),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> ping();
}
