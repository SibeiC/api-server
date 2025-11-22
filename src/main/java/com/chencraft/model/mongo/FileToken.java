package com.chencraft.model.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document("files")
@Data
@NoArgsConstructor
public class FileToken {
    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed()
    private String filename;

    private Instant issuedAt;
    private Instant usedAt;

    private boolean isDeleted = false;

    @Version
    private Long version;

    public FileToken(String filename) {
        this.token = UUID.randomUUID().toString();
        this.filename = filename;
        this.issuedAt = Instant.now();
    }
}
