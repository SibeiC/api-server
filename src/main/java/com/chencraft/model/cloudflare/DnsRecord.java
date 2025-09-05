package com.chencraft.model.cloudflare;


import com.chencraft.model.DDNSRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class DnsRecord {
    @JsonProperty
    private final String name;
    
    @JsonProperty
    private final int ttl = 1;

    @JsonProperty
    private final String type;

    @JsonProperty
    private final Boolean proxied;

    @JsonProperty
    private final String content;

    public DnsRecord(DDNSRequest request) {
        this.name = request.getHostname();
        this.type = request.getDnsType();
        this.proxied = request.isProxied();
        this.content = request.getMyIp();
    }
}
