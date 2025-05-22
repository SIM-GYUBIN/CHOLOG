package com.ssafy.cholog.global.config.webhook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
@EnableRetry
public class WebhookConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
