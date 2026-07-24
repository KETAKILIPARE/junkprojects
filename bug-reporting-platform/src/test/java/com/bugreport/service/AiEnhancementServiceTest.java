package com.bugreport.service;

import com.bugreport.domain.BugSeverity;
import com.bugreport.dto.AiEnhancedBug;
import com.bugreport.exception.AiEnhancementException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEnhancementServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiEnhancementService aiEnhancementService;

    private final ObjectMapper realObjectMapper = new ObjectMapper();

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        var objectMapperField = AiEnhancementService.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(aiEnhancementService, realObjectMapper);

        var apiKeyField = AiEnhancementService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(aiEnhancementService, "test-key");

        var apiUrlField = AiEnhancementService.class.getDeclaredField("apiUrl");
        apiUrlField.setAccessible(true);
        apiUrlField.set(aiEnhancementService, "https://api.openai.com/v1/chat/completions");

        var modelField = AiEnhancementService.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(aiEnhancementService, "gpt-4o-mini");
    }

    @Test
    void enhance_shouldReturnStructuredBug_whenOpenAiRespondsSuccessfully() {
        String mockApiResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"steps_to_reproduce\\":\\"1. Go to profile\\\\n2. Click save\\",\\"expected_behavior\\":\\"Email updates\\",\\"actual_behavior\\":\\"Email stays same\\",\\"severity\\":\\"MEDIUM\\",\\"suggested_labels\\":[\\"frontend\\",\\"profile\\"]}"
                    }
                  }]
                }
                """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn(mockApiResponse);

        AiEnhancedBug result = aiEnhancementService.enhance("When I click save nothing happens");

        assertThat(result.severity()).isEqualTo(BugSeverity.MEDIUM);
        assertThat(result.suggestedLabels()).contains("frontend");
        assertThat(result.expectedBehavior()).isEqualTo("Email updates");
    }

    @Test
    void enhance_shouldThrowAiEnhancementException_whenOpenAiCallFails() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> aiEnhancementService.enhance("some description"))
                .isInstanceOf(AiEnhancementException.class);
    }

    @Test
    void enhance_shouldThrowAiEnhancementException_whenResponseIsNull() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn(null);

        assertThatThrownBy(() -> aiEnhancementService.enhance("some description"))
                .isInstanceOf(AiEnhancementException.class);
    }
}
