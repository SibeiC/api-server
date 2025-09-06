package com.chencraft.common.service.api;

import com.chencraft.model.DDNSRequest;
import com.chencraft.model.cloudflare.CloudflareResponse;
import com.chencraft.model.cloudflare.DnsRecord;
import com.chencraft.model.cloudflare.IRecordResponse;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Low-level WebClient wrapper for Cloudflare DNS API.
 * External IO: HTTPS requests to Cloudflare; maps responses to CloudflareResponse and models.
 * Configuration: uses cloudflare.dns.apiKey and cloudflare.dns.zoneId properties.
 */
@Lazy
@Slf4j
@Service
public class CloudflareWebClient {
    private static final int DEFAULT_TIMEOUT = 60;

    private final WebClient webClient;

    @Value("${cloudflare.dns.zoneId:}")
    private String zoneId;

    /**
     * Constructs CloudflareWebClient with base URL and default headers.
     *
     * @param webClient base WebClient to mutate
     * @param cloudflareApiKey API token for Cloudflare (Bearer)
     */
    @Autowired
    public CloudflareWebClient(WebClient webClient, @Value("${cloudflare.dns.apiKey:}") String cloudflareApiKey) {
        String cloudflareApiBaseUrl = "https://api.cloudflare.com/client/v4";
        this.webClient = webClient.mutate()
                                  .baseUrl(cloudflareApiBaseUrl)
                                  .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cloudflareApiKey)
                                  .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                  .build();
    }

    /**
     * <a href="https://developers.cloudflare.com/api/resources/dns/subresources/records/methods/list/">List DNS Records</a>
     */
    public List<IRecordResponse> listDnsRecords(String hostname, String recordType) {
        String url = String.format("/zones/%s/dns_records", this.zoneId);
        CloudflareResponse<List<IRecordResponse>>
                response = webClient.get()
                                    .uri(uriBuilder ->
                                                 uriBuilder.path(url)
                                                           .queryParam("match", "all")
                                                           .queryParam("name.exact", hostname)
                                                           .queryParam("type", recordType)
                                                           .build())
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CloudflareResponse<List<IRecordResponse>>>() {
                                    })
                                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT));
        validateResponse(response);
        return response.getResult();
    }

    /**
     * <a href="https://developers.cloudflare.com/api/resources/dns/subresources/records/methods/update/">Overwrite DNS Record</a>
     */
    public void overwriteDnsRecord(DDNSRequest request, @Nonnull String recordId) {
        String url = String.format("/zones/%s/dns_records/%s", this.zoneId, recordId);
        CloudflareResponse<Map<String, Object>>
                response = webClient.put()
                                    .uri(uriBuilder -> uriBuilder.path(url).build())
                                    .bodyValue(new DnsRecord(request))
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CloudflareResponse<Map<String, Object>>>() {
                                    })
                                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT));
        validateResponse(response);
    }

    /**
     * <a href="https://developers.cloudflare.com/api/resources/dns/subresources/records/methods/create/">Create DNS Record</a>
     */
    public void createDnsRecordDetail(DDNSRequest request) {
        String url = String.format("/zones/%s/dns_records", this.zoneId);
        CloudflareResponse<Map<String, Object>>
                response = webClient.post()
                                    .uri(uriBuilder -> uriBuilder.path(url).build())
                                    .bodyValue(new DnsRecord(request))
                                    .retrieve()
                                    .bodyToMono(new ParameterizedTypeReference<CloudflareResponse<Map<String, Object>>>() {
                                    })
                                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT));
        validateResponse(response);
    }

    private void validateResponse(CloudflareResponse<?> response) {
        if (response == null) {
            throw new RuntimeException("CloudflareResponse is null");
        }
        if (!response.isSuccess() || (response.getErrors() != null && !response.getErrors().isEmpty())) {
            log.error("Encountered error in Cloudflare API: {}", response.getErrors());
            throw new RuntimeException("Cloudflare API request has failed");
        }
    }
}
