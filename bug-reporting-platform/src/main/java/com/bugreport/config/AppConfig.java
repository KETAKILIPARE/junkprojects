package com.bugreport.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class AppConfig {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            request.getHeaders().set("Authorization", "Bearer " + openAiApiKey);
            request.getHeaders().set("Content-Type", "application/json");
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(List.of(authInterceptor));
        return restTemplate;
    }
}
