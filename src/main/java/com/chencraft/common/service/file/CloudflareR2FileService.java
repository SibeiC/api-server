package com.chencraft.common.service.file;

import com.chencraft.api.NotFoundException;
import com.chencraft.common.component.AppConfig;
import com.chencraft.common.config.S3Config;
import com.chencraft.model.FileUpload;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.Optional;

/**
 * FileService implementation backed by Cloudflare R2 (S3-compatible) using AWS SDK v2.
 * External IO: performs object storage operations; requires cloudflare.r2.bucket and S3Config.
 * Thread-safety: S3Client is thread-safe; this component is a Spring singleton.
 */
@Lazy
@Slf4j
@Component
public class CloudflareR2FileService implements FileService {
    private final S3Client s3Client;
    private final AppConfig appConfig;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    /**
     * Constructs CloudflareR2FileService using provided S3Config and application configuration.
     *
     * @param config    S3 endpoint and credentials for R2
     * @param appConfig app configuration for environment-specific behavior
     */
    @Autowired
    public CloudflareR2FileService(S3Config config, AppConfig appConfig) {
        this.s3Client = buildS3Client(config);
        this.appConfig = appConfig;
    }

    /**
     * Verifies connectivity to the configured bucket on startup; fails fast on misconfiguration.
     */
    @PostConstruct
    public void init() {
        try {
            // The API token does not permit for listBucket or createBucket, the bucket should already exist.
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Successfully connected to Cloudflare R2 Storage bucket: {}", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new RuntimeException("Bucket does not exist: " + bucketName, e);
            } else if (e.statusCode() == 403) {
                throw new RuntimeException("Bucket exists but access denied: " + bucketName, e);
            } else {
                throw new RuntimeException("Error checking bucket: " + bucketName, e);
            }
        }
    }

    /**
     * Stores a file into Cloudflare R2 under the given destination prefix.
     *
     * @param destination enum indicating PUBLIC or PRIVATE location
     * @param filename    object key name; must be non-empty
     * @param contentType MIME type, required
     * @param content     file bytes
     * @throws org.springframework.web.reactive.function.UnsupportedMediaTypeException when contentType/filename missing
     */
    @Override
    public void uploadFile(FileUpload.Type destination, String filename, String contentType, byte[] content) {
        contentType = Optional.ofNullable(contentType)
                              .orElseThrow(() -> new UnsupportedMediaTypeException("Content-Type is unknown"));

        // Build the S3 key (prefix and filename)
        String fullPath = createPath(destination, filename);
        log.info("Uploading file to S3: {}/{}", this.bucketName, fullPath);

        // Upload file
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(this.bucketName)
                                                            .key(fullPath)
                                                            .contentType(contentType)
                                                            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    }

    @Override
    public ResponseEntity<@NonNull Resource> downloadFile(FileUpload.Type destination, @Nonnull String filename) {
        String fullPath = createPath(destination, filename);
        log.info("Downloading file from S3: {}/{}", this.bucketName, fullPath);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(fullPath)
                                                            .build();
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            return ResponseEntity.ok()
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
                                 .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                 .body(new InputStreamResource(s3Object));
        } catch (NoSuchKeyException e) {
            throw new NotFoundException(fullPath);
        }
    }

    /**
     * Builds and configures the S3 client with R2-specific settings
     */
    private static S3Client buildS3Client(S3Config config) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
        );

        S3Configuration serviceConfiguration = S3Configuration.builder()
                                                              .pathStyleAccessEnabled(true)
                                                              // disable AWS4 chunked uploads
                                                              .chunkedEncodingEnabled(false)
                                                              .build();

        return S3Client.builder()
                       .endpointOverride(URI.create(config.getEndpoint()))
                       .credentialsProvider(StaticCredentialsProvider.create(credentials))
                       .region(Region.of("auto"))
                       .serviceConfiguration(serviceConfiguration)
                       .build();
    }

    private String createPath(FileUpload.Type destination, String filename) {
        String tmp = this.appConfig.isDev() ? "tmp/" : "";  // tmp directory for testing, gets auto cleaned every day
        filename = Optional.ofNullable(filename)
                           .orElseThrow(() -> new UnsupportedMediaTypeException("Filename is missing"));
        return destination.toPrefix() + tmp + filename;
    }
}