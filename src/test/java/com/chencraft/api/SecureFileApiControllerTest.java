package com.chencraft.api;


import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.mongo.FileTokenRepository;
import com.chencraft.model.mongo.FileToken;
import com.chencraft.utils.FileServiceTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(MongoConfig.class)
public class SecureFileApiControllerTest {
    private static final String TEST_FILE_PATH = "src/test/resources/private/SecurefileApiControllerTest.txt";
    private static final String SHARED_FILE_PATH = "src/test/resources/share/ShareTest.txt";

    private MockMvc mockMvc;

    @Autowired
    private FileTokenRepository tokenRepository;

    @BeforeEach
    public void setup(WebApplicationContext webApplicationContext) {
        FileServiceTestHelper.createFile(TEST_FILE_PATH);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                      .apply(springSecurity())
                                      .build();
    }

    // Test: 200 OK - Valid Credentials
    @Test
    public void testGetFileWithValidCredentials() throws Exception {
        mockMvc.perform(get("/secure/file/SecurefileApiControllerTest.txt")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isOk());
    }

    // Test: 401 Unauthorized - Missing Credentials
    @Test
    public void testGetFileWithoutCredentials() throws Exception {
        mockMvc.perform(get("/secure/file/SecurefileApiControllerTest.txt")
                                .header("X-Client-Verify", "NONE"))
               .andExpect(status().isUnauthorized());
    }

    // Test: 401 Unauthorized - Incorrect Credentials
    @Test
    public void testGetFileWithInvalidCredentials() throws Exception {
        mockMvc.perform(get("/secure/file/SecurefileApiControllerTest.txt")
                                .header("X-Client-Verify", "FAILED"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    public void testUploadShareFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ShareTest.txt", "text/plain", "Hello World!".getBytes());

        MvcResult result = mockMvc.perform(multipart("/secure/file")
                                                   .file(file)
                                                   .param("destination", "SHARE")
                                                   .header("X-Client-Verify", "SUCCESS")
                                                   .contentType(MediaType.MULTIPART_FORM_DATA)
                                                   .accept(MediaType.APPLICATION_JSON))
                                  .andExpect(status().isOk())
                                  .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        String url = mapper.readTree(responseBody).path("url").asText();

        // Basic assertion to ensure URL is present
        assertNotNull(url);
        assertFalse(url.isBlank());

        // Expect URL like: /file/share?token=<uuid>
        assertTrue(url.startsWith("/file/share?token="), "Unexpected share URL format: " + url);

        // Extract token query param robustly
        Pattern pattern = Pattern.compile("[?&]token=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        assertTrue(matcher.find(), "Token parameter not found in URL: " + url);
        String token = matcher.group(1);

        // Optional: sanity-check token shape (UUID-like)
        Pattern uuid = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        assertTrue(uuid.matcher(token).matches(), "Token is not a UUID: " + token);

        FileToken fileToken = tokenRepository.findByTokenAndIsDeletedFalse(token).blockFirst();
        assertNotNull(fileToken);
        assertEquals("ShareTest.txt", fileToken.getFilename());
    }

    // TODO: Delete
    @Test
    public void testDeleteFile() throws Exception {
        mockMvc.perform(delete("/secure/file/SecurefileApiControllerTest.txt")
                                .queryParam("namespace", "PRIVATE")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isOk());

        assertFalse(FileServiceTestHelper.fileExists(TEST_FILE_PATH), "File was not deleted");
    }

    @AfterEach
    public void cleanup() {
        FileServiceTestHelper.deleteFile(TEST_FILE_PATH);
        FileServiceTestHelper.deleteFile(SHARED_FILE_PATH);
    }
}
