package com.chencraft.model;

import com.chencraft.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Setter
@ToString
@Validated
@NotUndefined
@EqualsAndHashCode
public class FileUpload {
    public enum Type {
        PUBLIC,
        PRIVATE;

        @Override
        @JsonValue
        public String toString() {
            return this.name();
        }

        @JsonCreator
        public static Type fromValue(String text) {
            for (Type b : Type.values()) {
                if (b.name().equals(text)) {
                    return b;
                }
            }
            return null;
        }

        @JsonIgnore
        public String toPrefix() {
            return this.name().toLowerCase() + "/";
        }
    }

    @JsonProperty("file")
    private MultipartFile file = null;

    @JsonProperty("destination")
    private Type destination = null;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "File to upload")
    @NotNull
    public MultipartFile getFile() {
        return file;
    }

    @Schema(example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED, description = "File visibility setting")
    @NotNull
    public FileUpload.Type getDestination() {
        return destination;
    }
}
