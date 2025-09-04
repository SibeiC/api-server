package com.chencraft.api;

import com.chencraft.utils.FileServiceTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class FileApiControllerTest {
    private MockMvc mockMvc;

    private static final String TEST_FILE_PATH = "src/test/resources/public/FileApiControllerTest.txt";

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
    public void testGetFile() throws Exception {
        mockMvc.perform(get("/file/FileApiControllerTest.txt"))
               .andExpect(status().isOk());
    }

    @AfterAll
    public static void cleanup() {
        FileServiceTestHelper.deleteFile(TEST_FILE_PATH);
    }
}