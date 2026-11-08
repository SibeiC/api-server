package com.chencraft.api.secure;

import com.chencraft.common.config.MongoConfig;
import com.chencraft.common.service.mail.MailFlag;
import com.chencraft.common.service.mail.MailService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(MongoConfig.class)
public class SecureNotificationApiControllerTest {
    private static String testClientCertPem;

    private MockMvc mockMvc;

    @MockitoBean
    private MailService mailService;

    @Value("${app.alert.recipient}")
    private String recipient;

    @BeforeAll
    static void loadTestClientCert() throws Exception {
        try (InputStream in = SecureNotificationApiControllerTest.class.getResourceAsStream("/certs/test-client.crt.pem")) {
            Assertions.assertNotNull(in, "test-client.crt.pem fixture missing");
            // nginx folds PEM newlines to spaces when forwarding $ssl_client_cert
            testClientCertPem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r", "").replace("\n", " ").trim();
        }
    }

    @BeforeEach
    public void setUp(WebApplicationContext ctx) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    @Test
    public void sendsMailToFixedRecipientStampedWithCallerCN() throws Exception {
        String body = """
                {"subject": "Backup\\r\\ndone", "body": "All good.", "level": "WARNING"}""";

        mockMvc.perform(post("/secure/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                                .header("X-Client-Verify", "SUCCESS")
                                .header("X-Client-Cert", testClientCertPem))
               .andExpect(status().isAccepted());

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService, times(1)).sendMail(eq(recipient), eq(MailFlag.WARNING), subject.capture(), text.capture());
        Assertions.assertEquals("[test-device] Backup done", subject.getValue());
        Assertions.assertTrue(text.getValue().startsWith("All good."));
        Assertions.assertTrue(text.getValue().endsWith("Sent by device: test-device"));
    }

    @Test
    public void defaultsToInfoWithoutCertHeader() throws Exception {
        mockMvc.perform(post("/secure/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\"hi\",\"body\":\"there\"}")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isAccepted());

        verify(mailService, times(1)).sendMail(eq(recipient), eq(MailFlag.INFO),
                                               eq("[" + SecureNotificationApiController.UNKNOWN_SENDER + "] hi"), anyString());
    }

    @Test
    public void rejectsBlankSubject() throws Exception {
        mockMvc.perform(post("/secure/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\" \",\"body\":\"there\"}")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(mailService);
    }

    @Test
    public void rejectsUnknownLevel() throws Exception {
        mockMvc.perform(post("/secure/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\"hi\",\"body\":\"there\",\"level\":\"PANIC\"}")
                                .header("X-Client-Verify", "SUCCESS"))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(mailService);
    }

    @Test
    public void rejectsWithoutMtls() throws Exception {
        mockMvc.perform(post("/secure/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"subject\":\"hi\",\"body\":\"there\"}"))
               .andExpect(status().isUnauthorized());
        verifyNoInteractions(mailService);
    }
}
