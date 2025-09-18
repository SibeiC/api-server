package com.chencraft.common.service.api;

import com.chencraft.api.NotFoundException;
import com.chencraft.common.component.FileData;
import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.exception.InvalidSignatureException;
import com.chencraft.common.service.HashService;
import com.chencraft.common.service.file.FileService;
import com.chencraft.model.FileUpload;
import jakarta.annotation.Nullable;
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

    @Value("${github.user.name:}")
    private String githubUsername;

    /**
     * Constructs GitHubApiService.
     *
     * @param webClient   reactive HTTP client configured with GitHub base URL/headers
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
     * Validates GitHub webhook signature against the configured secret.
     *
     * @param signature X-Hub-Signature-256 header value
     * @param rawBody   raw request payload used for HMAC computation
     * @throws InvalidSignatureException when signature mismatch
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
     * @param url      GitHub asset API URL
     * @param filename file name to use when storing
     * @throws GitHubUnauthorizedException if token unauthorized
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
                                                                                 response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM).toString(),
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

    /**
     * Fetches a raw file from a GitHub repository by repo full name and path.
     * Uses the GitHub Contents API with vnd.github.raw Accept to get bytes and content-type.
     *
     * @param repo   full repo name like "owner/repo"
     * @param path   file path inside the repo (e.g. "dir/file.txt")
     * @param branch branch to grab the file against, leave empty for default
     * @return GitHubFile containing filename, contentType, and bytes
     */
    public FileData fetchRepoFile(String repo, String path, @Nullable String branch) {
        int timeout = 60;
        String url = "https://api.github.com/repos/" + githubUsername + "/" + repo + "/contents/" + path
                + (branch == null ? "" : "?ref=" + branch);

        log.info("Fetching repo file from GitHub: {}/{}/{} from {}", githubUsername, repo, path, branch == null);
        return webClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + gitHubReadOnlyToken)
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("Accept", "application/vnd.github.raw")
                        .exchangeToMono(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return response.bodyToMono(byte[].class)
                                               .map(bytes -> new FileData(path, bytes));
                            } else if (HttpStatus.NOT_FOUND.equals(response.statusCode())) {
                                return Mono.error(new NotFoundException(url));
                            } else if (HttpStatus.UNAUTHORIZED.equals(response.statusCode())) {
                                return Mono.error(new GitHubUnauthorizedException("Unauthorized request to GitHub"));
                            } else {
                                log.error("Failed to fetch repo file from GitHub: {} {}", response.statusCode(), url);
                                return response.createException().flatMap(Mono::error);
                            }
                        })
                        .block(Duration.ofSeconds(timeout));
    }
}
