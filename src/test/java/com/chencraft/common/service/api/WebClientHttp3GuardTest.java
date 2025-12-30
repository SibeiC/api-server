package com.chencraft.common.service.api;

import com.chencraft.common.config.WebClientConfig;
import io.netty.util.internal.NativeLibraryLoader;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Guard test: our WebClient must work even if QUIC (HTTP/3) native libraries are unavailable.
 * We simulate an environment with missing QUIC natives by intercepting Netty's native loader
 * (io.netty.util.internal.NativeLibraryUtil) and throwing UnsatisfiedLinkError only for
 * QUIC-related libraries. This makes the test stable on machines with or without real HTTP/3 support
 * and avoids mocking java.lang.System, which Mockito forbids.
 */
public class WebClientHttp3GuardTest {

    @Test
    public void webClientShouldWorkWhenQuicNativeMissing_simulated() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();

            WebClient client = new WebClientConfig().webClient();
            String url = server.url("/ping").toString();

            // Simulate an environment where QUIC natives are missing by throwing for any QUIC-related library loads
            try (MockedStatic<NativeLibraryLoader> loader = Mockito.mockStatic(NativeLibraryLoader.class, InvocationOnMock::callRealMethod)) {
                // Intercept the typical Netty native-loading entry point for names containing "quic"/"quiche"
                loader.when(() -> NativeLibraryLoader.load(Mockito.argThat(name -> name != null && name.toLowerCase()
                                                                                                       .contains("quic")), Mockito.any()))
                      .thenThrow(new UnsatisfiedLinkError("Simulated missing QUIC native lib"));

                String body = client.get()
                                    .uri(url)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .block(Duration.ofSeconds(5));

                Assertions.assertEquals("ok", body);
            }
        }
    }
}
