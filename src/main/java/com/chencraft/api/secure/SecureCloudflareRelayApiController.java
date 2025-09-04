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

@Slf4j
@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureCloudflareRelayApiController implements SecureCloudflareRelayApi {
    private final CloudflareApiService cloudflareApiService;
    private final HttpServletRequest request;

    @Autowired
    public SecureCloudflareRelayApiController(CloudflareApiService cloudflareApiService, HttpServletRequest request) {
        this.cloudflareApiService = cloudflareApiService;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> relayDDNSRequest(DDNSRequest ddnsRequest) {
        log.warn(ddnsRequest.toString());
        if (ddnsRequest.getMyIp() == null) {
            ddnsRequest.setMyIp(request.getRemoteAddr());
        }

        this.cloudflareApiService.updateDNSRecord(ddnsRequest);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
