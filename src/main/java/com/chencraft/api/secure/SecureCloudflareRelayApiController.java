package com.chencraft.api.secure;

import com.chencraft.common.service.api.CloudflareApiService;
import com.chencraft.model.DDNSRequest;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Secure controller that relays DDNS update requests to Cloudflare. Requires mTLS authentication.
 * Uses CloudflareApiService for actual DNS updates.
 */
@Slf4j
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureCloudflareRelayApiController implements SecureCloudflareRelayApi {
    private final CloudflareApiService cloudflareApiService;
    private final HttpServletRequest request;

    /**
     * Constructs SecureCloudflareRelayApiController.
     *
     * @param cloudflareApiService service for Cloudflare DNS updates
     * @param request              current HttpServletRequest to infer client IP when not supplied
     */
    @Autowired
    public SecureCloudflareRelayApiController(CloudflareApiService cloudflareApiService, HttpServletRequest request) {
        this.cloudflareApiService = cloudflareApiService;
        this.request = request;
    }

    /**
     * Relays dynamic DNS update to Cloudflare. If client IP is omitted, the remote address is used.
     *
     * @param ddnsRequest payload containing zone, record details, and optional client IP
     * @return HTTP 200 after the update request is submitted
     */
    @Override
    public ResponseEntity<?> relayDDNSRequest(DDNSRequest ddnsRequest) {
        log.warn(ddnsRequest.toString());
        if (ddnsRequest.getMyIp() == null) {
            ddnsRequest.setMyIp(request.getRemoteAddr());
        }

        this.cloudflareApiService.updateDNSRecord(ddnsRequest);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
