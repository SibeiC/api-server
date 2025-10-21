package com.chencraft.api;

import com.chencraft.common.component.AlertMessenger;
import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.exception.InvalidSignatureException;
import com.chencraft.common.service.api.GitHubApiService;
import com.chencraft.common.service.executor.TaskExecutor;
import com.chencraft.model.GitHubWebhookRelease;
import com.chencraft.model.GitHubWebhookReleaseReleaseAssets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller handling GitHub webhook callbacks. Validates HMAC signature and dispatches
 * background tasks to fetch release assets. Errors are mapped to appropriate HTTP responses.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")

@RestController
@Slf4j
public class GithubWebhookApiController implements GithubWebhookApi {

    private final ObjectMapper objectMapper;
    private final GitHubApiService gitHubApiService;
    private final TaskExecutor taskExecutor;
    private final AlertMessenger messenger;

    /**
     * Constructs GithubWebhookApiController.
     *
     * @param objectMapper     Jackson ObjectMapper for parsing webhook bodies
     * @param gitHubApiService service for signature validation and asset fetching
     * @param taskExecutor     async executor to fetch assets without blocking the request thread
     * @param messenger        alerting component for token issues
     */
    @Autowired
    public GithubWebhookApiController(ObjectMapper objectMapper,
                                      GitHubApiService gitHubApiService,
                                      TaskExecutor taskExecutor,
                                      AlertMessenger messenger) {
        this.objectMapper = objectMapper;
        this.gitHubApiService = gitHubApiService;
        this.taskExecutor = taskExecutor;
        this.messenger = messenger;
    }

    /**
     * Webhook endpoint handling GitHub release events.
     *
     * @param signature X-Hub-Signature-256 header from GitHub (sha256=<hex>)
     * @param rawBody   raw request body for signature validation and JSON parsing
     * @return 200 OK when accepted, 403 if signature invalid, or error if body invalid
     */
    @Override
    public ResponseEntity<Void> githubUpdate(String signature, String rawBody) {
        try {
            gitHubApiService.validateHeaderSignature(signature, rawBody);

            GitHubWebhookRelease body = objectMapper.readValue(rawBody, GitHubWebhookRelease.class);
            if (body.getAction() == GitHubWebhookRelease.ActionEnum.PRERELEASED) {
                return new ResponseEntity<>(HttpStatus.OK);
            }

            taskExecutor.execute(() -> fetchFiles(body.getRepository().getFullName(), body.getRelease().getAssets()));

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (InvalidSignatureException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse body: {}", rawBody, e);
            throw new RuntimeException(e);
        }
    }

    private void fetchFiles(String repoName, List<GitHubWebhookReleaseReleaseAssets> assets) {
        try {
            for (GitHubWebhookReleaseReleaseAssets asset : assets) {
                String downloadUrl = asset.getUrl();
                String filename = asset.getName();
                gitHubApiService.fetchFileFromGitHub(downloadUrl, filename);
            }
        } catch (GitHubUnauthorizedException e) {
            messenger.alertUnauthorizedGitHubToken(repoName);
        }
    }
}
