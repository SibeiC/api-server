package com.chencraft.api.secure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
public class SecurePingApiControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                      .apply(springSecurity())
                                      .build();
    }

    @Test
    public void testUnauthorized() throws Exception {
        mockMvc.perform(get("/secure/ping"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPing() throws Exception {
        mockMvc.perform(get("/secure/ping")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isOk())
               .andExpect(content().string("pong"));
    }
}