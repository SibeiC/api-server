package com.chencraft.api.secure;

import com.chencraft.model.HealthCheckTargetRequest;
import com.chencraft.model.mongo.HealthCheckTarget;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.HEALTHCHECK;

@Validated
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public interface SecureHealthCheckApi {

    @Operation(summary = "List monitored health-check targets",
            description = "Returns all registered health-check targets along with their latest probe outcome.",
            security = {@SecurityRequirement(name = "mTLS")}, tags = {HEALTHCHECK})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of registered targets"),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/healthcheck/targets",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    Flux<@NonNull HealthCheckTarget> listTargets();

    @Operation(summary = "Register a new health-check target",
            description = "Adds a URL to the monitored set. The scheduled sweep will start probing it on the next tick.",
            security = {@SecurityRequirement(name = "mTLS")}, tags = {HEALTHCHECK})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Target registered"),
            @ApiResponse(responseCode = "400", ref = INVALID_INPUT_RESPONSE),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/healthcheck/targets",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST)
    Mono<@NonNull ResponseEntity<@NonNull HealthCheckTarget>> addTarget(
            @Parameter(in = ParameterIn.DEFAULT, description = "Target to register",
                    schema = @Schema(implementation = HealthCheckTargetRequest.class))
            @NotNull @Valid @RequestBody HealthCheckTargetRequest request);

    @Operation(summary = "Delete a health-check target by id",
            description = "Soft-deletes the target so the scheduler will stop probing it.",
            security = {@SecurityRequirement(name = "mTLS")}, tags = {HEALTHCHECK})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Target removed"),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(responseCode = "404", description = "Target not found"),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/healthcheck/targets/{id}",
            method = RequestMethod.DELETE)
    Mono<@NonNull ResponseEntity<@NonNull String>> deleteTarget(
            @PathVariable @Parameter(in = ParameterIn.PATH, description = "Mongo id of the target to delete", required = true) String id);
}
