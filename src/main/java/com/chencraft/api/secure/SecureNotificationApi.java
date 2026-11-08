package com.chencraft.api.secure;

import com.chencraft.model.NotificationRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.NOTIFICATION;

@Validated
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public interface SecureNotificationApi {

    @Operation(summary = "Send a notification email",
            description = "Relays a notification through the iCloud SMTP route to the configured alert recipient. "
                    + "The sending device (client cert CN) is stamped into the subject and body. Delivery is asynchronous.",
            security = {@SecurityRequirement(name = "mTLS")}, tags = {NOTIFICATION})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Notification queued for delivery"),
            @ApiResponse(responseCode = "400", ref = INVALID_INPUT_RESPONSE),
            @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)
    })
    @RequestMapping(value = "/notify",
            produces = {MediaType.TEXT_PLAIN_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST)
    ResponseEntity<@NonNull String> sendNotification(
            @Parameter(in = ParameterIn.DEFAULT, description = "Notification to send",
                    schema = @Schema(implementation = NotificationRequest.class))
            @NotNull @Valid @RequestBody NotificationRequest notification);
}
