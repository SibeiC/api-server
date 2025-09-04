package com.chencraft.common.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                                          .followRedirect(true)
                                          .wiretap("reactor.netty.http.client.HttpClient",
                                                   LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        return WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }
}
