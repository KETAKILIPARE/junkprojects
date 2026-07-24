package com.bugreport.service;

import com.bugreport.dto.AiEnhancedBug;
import com.bugreport.exception.AiEnhancementException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bugreport.domain.BugSeverity;

@Service
@RequiredArgsConstructor
public class AiEnhancementService {

    private static final String SYSTEM_PROMPT = """
            You are a bug report analyzer. Given a raw bug description, extract and return ONLY a JSON object with these fields:
            - steps_to_reproduce: string
            - expected_behavior: string
            - actual_behavior: string
            - severity: one of LOW, MEDIUM, HIGH, CRITICAL
            - suggested_labels: array of strings
            Return only valid JSON, no markdown, no explanation.
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.api-url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public AiEnhancedBug enhance(String rawDescription) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", rawDescription)
                    )
            );

            String response = restTemplate.postForObject(apiUrl, requestBody, String.class);

            if (response == null) {
                throw new AiEnhancementException("OpenAI returned null response");
            }

            return parseAiResponse(response);
        } catch (AiEnhancementException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiEnhancementException("AI enhancement failed: " + ex.getMessage());
        }
    }

    private AiEnhancedBug parseAiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        JsonNode parsed = objectMapper.readTree(content);

        List<String> labels = new ArrayList<>();
        parsed.path("suggested_labels").forEach(label -> labels.add(label.asText()));

        return new AiEnhancedBug(
                parsed.path("steps_to_reproduce").asText(),
                parsed.path("expected_behavior").asText(),
                parsed.path("actual_behavior").asText(),
                BugSeverity.valueOf(parsed.path("severity").asText()),
                labels
        );
    }
}
