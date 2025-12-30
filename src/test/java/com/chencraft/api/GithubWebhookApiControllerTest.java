package com.chencraft.api;

import com.chencraft.common.exception.GitHubUnauthorizedException;
import com.chencraft.common.service.mail.MailFlag;
import com.chencraft.common.service.mail.MailService;
import com.chencraft.utils.FileServiceTestHelper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class GithubWebhookApiControllerTest {
    private static final String TEST_FILE_PATH = "src/test/resources/public/webDownload.pdf";

    private static final String REPO_DIR = "src/test/resources/testRepo/";

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @MockitoSpyBean
    private WebClient webClient;

    @MockitoSpyBean
    private MailService mailService;

    @Mock
    WebClient.RequestHeadersUriSpec requestSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;

    @BeforeAll
    public static void beforeAll() {
        FileServiceTestHelper.deleteFile(TEST_FILE_PATH);
        FileServiceTestHelper.createDirectory(REPO_DIR);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp(WebApplicationContext webApplicationContext) {
        // Mockito setup
        closeable = MockitoAnnotations.openMocks(this);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);

        // Chain the WebClient mocks
        when(webClient.get()).thenReturn(requestSpec);
        when(headersSpec.header(eq("Authorization"), eq("Bearer dummy-token"))).thenReturn(headersSpec);
        when(headersSpec.header(eq("X-GitHub-Api-Version"), eq("2022-11-28"))).thenReturn(headersSpec);
        when(headersSpec.accept(MediaType.APPLICATION_OCTET_STREAM)).thenReturn(headersSpec);

        // Stub the final call
        when(requestSpec.uri(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            Assertions.assertEquals("https://example.com/a.pdf", url); // verify correct URL
            return headersSpec;
        });
        when(headersSpec.exchangeToMono(any()))
                .thenReturn(Mono.just(new byte[0]));

        // MockMVC setup
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                      .apply(springSecurity())
                                      .build();
    }

    @Test
    public void testgithubUpdate_Prereleased() throws Exception {
        // Create the release object
        String releaseNotification = """
                {\s
                    "action": "prereleased"
                }""";

        mockMvc.perform(post("/webhook/github/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(releaseNotification)
                                .header("X-Hub-Signature-256", "sha256=ab45909d1722c1a857b10f41d9e72e3333bdd5af5caf5c1f3680d7613821f1bf"))
               .andExpect(status().isOk());

        // Assert
        verifyNoInteractions(webClient.get());
    }

    @Test
    public void testgithubUpdate_NoAssetsList() throws Exception {
        // Create the release object
        String releaseNotification = """
                {\s
                    "action": "released",
                    "release": {
                        "assets": []
                    },
                    "repository": {
                        "full_name": "SibeiC/api-server"
                    }
                }""";

        mockMvc.perform(post("/webhook/github/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(releaseNotification)
                                .header("X-Hub-Signature-256", "sha256=99fb1ec287032923b7794e1d873012f00cf00f0711ce06c5bc1a580d8e9723b2"))
               .andExpect(status().isOk());

        // Assert
        verifyNoInteractions(webClient.get());
    }

    @Test
    public void testgithubUpdate_NormalRelease() throws Exception {
        // Create the release object
        String releaseNotification = """
                {\s
                    "action": "released",
                    "release": {
                        "assets": [{
                            "url": "https://example.com/a.pdf",
                            "name": "a.pdf"
                        }]
                    },
                    "repository": {
                        "full_name": "SibeiC/api-server"
                    }
                }""";

        mockMvc.perform(post("/webhook/github/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(releaseNotification)
                                .header("X-Hub-Signature-256", "sha256=2e3a2bb6a8d6b1cc5665ef6660865452bfd46d3d0d61cb610bcdaa7f43f8a5e2"))
               .andExpect(status().isOk());

        // Assert
        verify(webClient, times(1)).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testgithubUpdate_Unauthorized() throws Exception {
        // Create the release object
        String releaseNotification = """
                {\s
                    "action": "released",
                    "release": {
                        "assets": [{
                            "url": "https://example.com/a.pdf",
                            "name": "a.pdf"
                        }]
                    },
                    "repository": {
                        "full_name": "SibeiC/api-server"
                    }
                }""";

        when(headersSpec.exchangeToMono(any())).thenThrow(new GitHubUnauthorizedException("Unauthorized"));

        mockMvc.perform(post("/webhook/github/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(releaseNotification)
                                .header("X-Hub-Signature-256", "sha256=2e3a2bb6a8d6b1cc5665ef6660865452bfd46d3d0d61cb610bcdaa7f43f8a5e2"))
               .andExpect(status().isOk());

        // Assert
        verify(webClient, times(1)).get();
        verify(mailService, times(1)).sendMail(anyString(), any(MailFlag.class), anyString(), anyString());
    }

    @Test
    public void testgithubUpdate_InvalidSignature() throws Exception {
        // Create the release object
        String releaseNotification = """
                {\s
                    "action": "released",
                    "release": {
                        "assets": [{
                            "url": "https://example.com/a.pdf",
                            "name": "a.pdf"
                        }]
                    },
                    "repository": {
                        "full_name": "SibeiC/api-server"
                    }
                }""";

        mockMvc.perform(post("/webhook/github/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(releaseNotification)
                                .header("X-Hub-Signature-256", "sha256=you-are-hacked"))
               .andExpect(status().isForbidden());

        // Assert
        verifyNoInteractions(webClient);
    }

    @Test
    public void testFollowRedirect() throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            // Step 1: enqueue redirect response
            server.enqueue(new MockResponse().setResponseCode(302)
                                             .addHeader("Location", "/final"));

            // Step 2: enqueue final destination response
            server.enqueue(new MockResponse().setResponseCode(200)
                                             .setBody("success"));

            server.start();

            when(webClient.get()).thenCallRealMethod(); // Remove stubbing
            String body = webClient.get()
                                   .uri(server.url("/redirect").toString())
                                   .retrieve()
                                   .bodyToMono(String.class)
                                   .block();

            Assertions.assertEquals("success", body);

            // Assert redirect was followed by checking request count
            Assertions.assertEquals(2, server.getRequestCount());
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @AfterAll
    public static void afterAll() {
        FileServiceTestHelper.deleteDirectory(REPO_DIR);
    }
}
