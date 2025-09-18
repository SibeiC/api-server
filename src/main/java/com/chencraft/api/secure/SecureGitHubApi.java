package com.chencraft.api.secure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.GITHUB;

@Validated
public interface SecureGitHubApi {

    @Operation(
            summary = "Fetch a file from a private GitHub repo",
            description = "Fetches the raw file bytes from GitHub using a read-only token. See [GitHub API Docs](https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28#get-repository-content) for details.",
            tags = {GITHUB})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File bytes returned"),
            @ApiResponse(responseCode = "401", ref = INVALID_INPUT_RESPONSE),
            @ApiResponse(responseCode = "404", ref = FILE_NOT_FOUND_RESPONSE),
            @ApiResponse(ref = INTERNAL_SERVER_ERROR_RESPONSE)})
    @RequestMapping(value = "/github/file",
            method = RequestMethod.GET)
    ResponseEntity<byte[]> fetchGithubFile(
            @NotNull @Parameter(in = ParameterIn.QUERY, description = "Repository name (case insensitive)", required = true, schema = @Schema()) @Valid @RequestParam("repo") String repo,
            @NotNull @Parameter(in = ParameterIn.QUERY, description = "Full file path (case sensitive)", required = true, schema = @Schema()) @Valid @RequestParam("path") String path,
            @Parameter(in = ParameterIn.QUERY, description = "Branch name (default: master)", schema = @Schema()) @RequestParam(value = "branch", required = false) String branch);
}
