package com.chencraft.api.secure;

import com.chencraft.common.component.FileData;
import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.service.api.GitHubApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(MongoConfig.class)
class SecureGitHubApiControllerTest {
    private MockMvc mockMvc;

    @MockitoSpyBean
    private GitHubApiService gitHubApiService;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                      .apply(springSecurity())
                                      .build();
    }

    @Test
    public void testFetchGithubFile_MissingParams_BadRequest() throws Exception {
        mockMvc.perform(get("/secure/github/file")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchGithubFile_Unauthorized() throws Exception {
        mockMvc.perform(get("/secure/github/file")
                                .param("repo", "my-repo")
                                .param("path", "README.md"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFetchGithubFile_GitHubUnauthorized() throws Exception {
        doThrow(new GitHubUnauthorizedException("Unauthorized"))
                .when(gitHubApiService).fetchRepoFile(anyString(), anyString(), any());

        mockMvc.perform(get("/secure/github/file")
                                .header("X-Client-Verify", "SUCCESS")
                                .param("repo", "my-repo")
                                .param("path", "README.md"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFetchGithubFile_Success() throws Exception {
        doReturn(new FileData("filename.txt", new byte[0]))
                .when(gitHubApiService).fetchRepoFile(anyString(), anyString(), any());

        mockMvc.perform(get("/secure/github/file")
                                .header("X-Client-Verify", "SUCCESS")
                                .param("repo", "my-repo")
                                .param("path", "README.md"))
               .andExpect(status().isOk());
    }
}