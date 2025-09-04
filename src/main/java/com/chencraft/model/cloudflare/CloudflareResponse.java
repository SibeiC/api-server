package com.chencraft.model.cloudflare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@NoArgsConstructor
public class CloudflareResponse<RT> {
    @JsonProperty
    private List<String> errors;

    @JsonProperty
    private List<String> messages;

    @JsonProperty
    private boolean success;

    @JsonProperty
    private RT result;
}
