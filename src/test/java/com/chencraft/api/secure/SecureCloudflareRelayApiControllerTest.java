package com.chencraft.api.secure;

import com.chencraft.common.service.api.CloudflareApiService;
import com.chencraft.model.DDNSRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecureCloudflareRelayApiControllerTest {
    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @MockitoBean
    private CloudflareApiService cfApiService;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext) {
        // Mockito setup
        closeable = MockitoAnnotations.openMocks(this);

        // MockMVC setup
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                      .apply(springSecurity())
                                      .build();
    }

    @Test
    public void test200() throws Exception {
        String body = """
                {
                  "hostname": "www.chencraft.com",
                  "dnsType": "A",
                  "myIp": "1.1.1.1",
                  "proxied": false
                }""";

        mockMvc.perform(put("/secure/cloudflare/ddns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                                .content(body)
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isOk());
        verify(cfApiService, times(1)).updateDNSRecord(any());
    }

    @Test
    public void test200WithNoIp() throws Exception {
        String body = """
                {
                  "hostname": "www.chencraft.com",
                  "dnsType": "A",
                  "proxied": false
                }""";

        mockMvc.perform(put("/secure/cloudflare/ddns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                                .content(body)
                                .header("X-Client-Verify", "SUCCESS")
                                .with((RequestPostProcessor) request -> {
                                    request.setRemoteAddr("8.8.8.8");
                                    return request;
                                }))
               .andExpect(status().isOk());
        ArgumentCaptor<DDNSRequest> requestCaptor = ArgumentCaptor.forClass(DDNSRequest.class);
        verify(cfApiService, times(1)).updateDNSRecord(requestCaptor.capture());
        Assertions.assertEquals("8.8.8.8", requestCaptor.getValue().getMyIp());
    }

    @Test
    public void test400() throws Exception {
        String body = """
                {
                  "dnsType": "A",
                  "myIp": "1.1.1.1",
                  "proxied": false
                }""";

        mockMvc.perform(put("/secure/cloudflare/ddns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                                .content(body)
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(cfApiService);
    }

    @Test
    public void test401() throws Exception {
        String body = """
                {
                  "hostname": "www.chencraft.com",
                  "dnsType": "A",
                  "myIp": "1.1.1.1",
                  "proxied": false
                }""";

        mockMvc.perform(put("/secure/cloudflare/ddns")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                                .content(body))
               .andExpect(status().isUnauthorized());
        verifyNoInteractions(cfApiService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }
}