package com.chencraft.model.cloudflare;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ARecordResponse.class, name = "A"),
        @JsonSubTypes.Type(value = AAAARecordResponse.class, name = "AAAA"),
        @JsonSubTypes.Type(value = CNAMERecordResponse.class, name = "CNAME")
})
public interface IRecordResponse {
    String getId();

    String getName();

    String getType();

    String getContent();

    boolean isProxiable();

    boolean isProxied();
}
