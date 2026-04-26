package com.chencraft.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.chencraft.api.models.ResponseConstants.INTERNAL_SERVER_ERROR_RESPONSE;
import static com.chencraft.api.models.ResponseConstants.OK_RESPONSE;
import static com.chencraft.api.models.TagConstants.HEALTHCHECK;

@Validated
public interface HealthCheckApi {

    @Operation(summary = "Self health check", description = "Lightweight liveness probe for this server.", tags = {HEALTHCHECK})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", ref = OK_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/healthcheck", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<@NonNull String> healthCheck();
}
