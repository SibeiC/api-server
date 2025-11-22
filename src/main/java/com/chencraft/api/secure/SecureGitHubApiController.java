package com.chencraft.api.secure;

import com.chencraft.common.component.FileData;
import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.service.api.GitHubApiService;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityScheme(type = SecuritySchemeType.MUTUALTLS, name = "mTLS")
public class SecureGitHubApiController implements SecureGitHubApi {
    private final GitHubApiService gitHubApiService;

    @Autowired
    public SecureGitHubApiController(GitHubApiService gitHubApiService) {
        this.gitHubApiService = gitHubApiService;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public ResponseEntity<byte[]> fetchGithubFile(String repo, String path, String branch) {
        try {
            FileData file = gitHubApiService.fetchRepoFile(repo, path, branch);
            return ResponseEntity.ok()
                                 .header("Content-Type", file.contentType())
                                 .header("Content-Disposition", "inline; filename=\"" + file.filename() + "\"")
                                 .body(file.bytes());
        } catch (GitHubUnauthorizedException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }
}
