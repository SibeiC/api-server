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

    @Autowired
    public GitHubApiService(WebClient webClient,
                            FileService fileService,
                            HashService hashService) {
        this.webClient = webClient;
        this.fileService = fileService;
        this.hashService = hashService;
    }

    public void validateHeaderSignature(String signature, String rawBody) {
        // Verify request is genuinely sent by GitHub
        if (!hashService.validGitHubSignature(signature, rawBody, gitHubWebhookSecret)) {
            throw new InvalidSignatureException("GitHub header signature verification failed");
        }
    }

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
