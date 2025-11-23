package com.chencraft.api;

import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.service.file.FileTokenService;
import com.chencraft.utils.FileServiceTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FileApiControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private FileTokenService fileTokenService;

    private static final String TEST_FILE_PATH = "src/test/resources/public/FileApiControllerTest.txt";
    private static final String SHARED_FILE_PATH = "src/test/resources/share/ShareTest.txt";

    @BeforeAll
    public static void setupAll() {
        FileServiceTestHelper.createFile(TEST_FILE_PATH);
        FileServiceTestHelper.createFile(SHARED_FILE_PATH);
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

    @Test
    public void testGetShareFile() throws Exception {
        String url = fileTokenService.generateAccessToken("ShareTest.txt");
        mockMvc.perform(get(url))
               .andExpect(status().isOk());
    }

    @Test
    public void testGetShareFile_MissingFile() throws Exception {
        mockMvc.perform(get("/file/share?token=1234"))
               .andExpect(status().isNotFound());
    }

    @Test
    public void testGetShareFile_MissingToken() throws Exception {
        mockMvc.perform(get("/file/share"))
               .andExpect(status().isBadRequest());
    }

    @AfterAll
    public static void cleanup() {
        FileServiceTestHelper.deleteFile(TEST_FILE_PATH);
        FileServiceTestHelper.deleteFile(SHARED_FILE_PATH);
    }
}
