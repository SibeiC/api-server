package com.chencraft.common.service.api;

import com.chencraft.model.DDNSRequest;
import com.chencraft.model.cloudflare.IRecordResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CloudflareApiService {
    private final CloudflareWebClient cloudflareWebClient;

    @Autowired
    public CloudflareApiService(CloudflareWebClient cloudflareWebClient) {
        this.cloudflareWebClient = cloudflareWebClient;
    }

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
