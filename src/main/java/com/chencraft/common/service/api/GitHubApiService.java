package com.chencraft.common.service.api;

import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.exception.InvalidSignatureException;
import com.chencraft.common.service.HashService;
import com.chencraft.common.service.file.FileService;
import com.chencraft.model.FileUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service integrating with GitHub API for validating webhooks and downloading release assets.
 * External IO: performs HTTPS calls to api.github.com via WebClient and writes to storage via FileService.
 * Thread-safety: stateless aside from configuration properties; WebClient is thread-safe.
 */
@Slf4j
@Service
public class GitHubApiService {
    private final WebClient webClient;

    private final FileService fileService;

    private final HashService hashService;

    @Value("${github.readonly.token}")
    private String gitHubReadOnlyToken;

    @Value("${github.webhook.secret}")
    private String gitHubWebhookSecret;

    /**
     * Constructs GitHubApiService.
     *
     * @param webClient reactive HTTP client configured with GitHub base URL/headers
     * @param fileService storage service to persist downloaded assets
     * @param hashService hashing utility to validate webhook signatures
     */
    @Autowired
    public GitHubApiService(WebClient webClient,
                            FileService fileService,
                            HashService hashService) {
        this.webClient = webClient;
        this.fileService = fileService;
        this.hashService = hashService;
    }

    /**
         * Validates GitHub webhook signature against configured secret.
         *
         * @param signature X-Hub-Signature-256 header value
         * @param rawBody raw request payload used for HMAC computation
         * @throws com.chencraft.common.exception.InvalidSignatureException when signature mismatch
         */
        public void validateHeaderSignature(String signature, String rawBody) {
        // Verify request is genuinely sent by GitHub
        if (!hashService.validGitHubSignature(signature, rawBody, gitHubWebhookSecret)) {
            throw new InvalidSignatureException("GitHub header signature verification failed");
        }
    }

    /**
         * Downloads a file from GitHub asset API and stores it via FileService under PUBLIC bucket.
         *
         * @param url GitHub asset API URL
         * @param filename file name to use when storing
         * @throws com.chencraft.common.exception.GitHubUnauthorizedException if token unauthorized
         */
        public void fetchFileFromGitHub(String url, String filename) {
        int timeout = 60;
        log.info("Fetching {} from GitHub: {}", filename, url);
        webClient.get()
                 .uri(url)
                 .header("Authorization", "Bearer " + gitHubReadOnlyToken)
                 .header("X-GitHub-Api-Version", "2022-11-28")
                 .accept(MediaType.APPLICATION_OCTET_STREAM)
                 .exchangeToMono(response -> {
                     if (response.statusCode().is2xxSuccessful()) {
                         return response.bodyToMono(byte[].class)
                                        .doOnNext(bytes ->
                                                          fileService.uploadFile(FileUpload.Type.PUBLIC, filename,
                                                                                 response.headers().contentType().orElseThrow().toString(),
                                                                                 bytes));
                     } else if (HttpStatus.UNAUTHORIZED.equals(response.statusCode())) {
                         return Mono.error(new GitHubUnauthorizedException("Unauthorized request to GitHub"));
                     } else {
                         log.error("Failed to fetch file from GitHub: {}", response.statusCode());
                         return response.createException()
                                        .flatMap(Mono::error);
                     }
                 })
                 .block(Duration.ofSeconds(timeout));
    }
}
