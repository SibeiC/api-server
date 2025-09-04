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

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
@RestController
@Slf4j
public class GithubWebhookApiController implements GithubWebhookApi {

    private final ObjectMapper objectMapper;
    private final GitHubApiService gitHubApiService;
    private final TaskExecutor taskExecutor;
    private final AlertMessenger messenger;

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

    @Override
    public ResponseEntity<Void> githubWebhookUpdate(String signature, String rawBody) {
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
