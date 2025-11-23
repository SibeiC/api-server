package com.chencraft.api;


import com.chencraft.common.config.MongoConfig;
import com.chencraft.utils.FileServiceTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(MongoConfig.class)
public class SecureFileApiControllerTest {
    private static final String TEST_FILE_PATH = "src/test/resources/private/SecurefileApiControllerTest.txt";
    private MockMvc mockMvc;

    @BeforeAll
    public static void setupAll() {
        FileServiceTestHelper.createFile(TEST_FILE_PATH);
    }

    @BeforeEach
    public void setup(WebApplicationContext webApplicationContext) {
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

    // TODO: Test Delete functions

    @AfterAll
    public static void cleanup() {
        FileServiceTestHelper.deleteFile(TEST_FILE_PATH);
    }
}
