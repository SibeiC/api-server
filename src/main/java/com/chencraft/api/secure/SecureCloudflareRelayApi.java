package com.chencraft.api.secure;

import com.chencraft.model.DDNSRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.CLOUDFLARE;

@Validated
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS)
public interface SecureCloudflareRelayApi {
    @Operation(summary = "Update DNS record in Cloudflare", description = "Automatically create and update DNS record for given hostname", security = {@SecurityRequirement(name = "mTLS")}, tags = {CLOUDFLARE})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", ref = OK_RESPONSE),
            @ApiResponse(responseCode = "400", ref = INVALID_INPUT_RESPONSE),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/cloudflare/ddns",
            produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.PUT)
    ResponseEntity<?> relayDDNSRequest(
            @Parameter(in = ParameterIn.DEFAULT, description = "DNS update request", schema = @Schema(implementation = DDNSRequest.class))
            @NotNull @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Visit [Cloudflare API docs](https://developers.cloudflare.com/api/resources/dns/subresources/records/methods/update/) for details.") @RequestBody DDNSRequest ddnsRequest
    );
}
