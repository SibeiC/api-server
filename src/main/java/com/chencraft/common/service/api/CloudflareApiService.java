package com.chencraft.common.service.api;

import com.chencraft.model.DDNSRequest;
import com.chencraft.model.cloudflare.IRecordResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade service for Cloudflare DNS updates. Decides whether to create or update a record
 * by inspecting existing DNS entries via CloudflareWebClient.
 */
@Slf4j
@Service
public class CloudflareApiService {
    private final CloudflareWebClient cloudflareWebClient;

    /**
     * Constructs CloudflareApiService.
     *
     * @param cloudflareWebClient low-level client to call Cloudflare REST API
     */
    @Autowired
    public CloudflareApiService(CloudflareWebClient cloudflareWebClient) {
        this.cloudflareWebClient = cloudflareWebClient;
    }

    /**
         * Creates or updates a DNS record in Cloudflare for the given request.
         * If no record exists, it will be created; otherwise the first match is updated.
         *
         * @param request DDNS parameters including hostname, DNS type, content and proxied flag
         */
        public void updateDNSRecord(DDNSRequest request) {
        List<IRecordResponse> records = this.cloudflareWebClient.listDnsRecords(request.getHostname(), request.getDnsType());
        if (records.isEmpty()) {
            // Create DNS record
            this.cloudflareWebClient.createDnsRecordDetail(request);
        } else {
            // Update DNS record
            IRecordResponse record = records.getFirst();
            if (request.isProxied() == null) {
                request.setProxied(record.isProxiable() && record.isProxied());
            }
            this.cloudflareWebClient.overwriteDnsRecord(request, record.getId());
        }
    }
}
