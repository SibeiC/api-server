package com.chencraft.model.cloudflare;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * <a href="https://developers.cloudflare.com/api/resources/dns/subresources/records/models/aaaa_record/#(schema)">Source schema</a>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AAAARecordResponse implements IRecordResponse {
    private String id;
    private String name;
    private String type;
    private String content;
    private boolean proxiable;
    private boolean proxied;
    private int ttl;
    private Map<String, Object> settings;
    private Map<String, Object> meta;
    private String comment;
    private List<String> tags;

    @JsonProperty("created_on")
    private OffsetDateTime createdOn;

    @JsonProperty("modified_on")
    private OffsetDateTime modifiedOn;
}
