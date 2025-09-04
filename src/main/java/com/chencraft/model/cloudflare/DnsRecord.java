package com.chencraft.model.cloudflare;


import com.chencraft.model.DDNSRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DnsRecord {
    @JsonProperty
    private String name;
    
    @JsonProperty
    private int ttl = 1;

    @JsonProperty
    private String type;

    @JsonProperty
    private boolean proxied;

    @JsonProperty
    private String content;

    public DnsRecord(DDNSRequest request) {
        this.name = request.getHostname();
        this.type = request.getDnsType();
        this.proxied = request.isProxied();
        this.content = request.getMyIp();
    }
}
