package com.meeran.newsanalyzerapi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeran.newsanalyzerapi.dto.AnalysisDto.ProblemAnalysis;
import com.meeran.newsanalyzerapi.dto.Article;
import com.meeran.newsanalyzerapi.dto.GeminiDto;

@Service
public class AnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // --- Primary Provider Config ---
    @Value("${llm.primary.api.key}")
    private String primaryApiKey;
    @Value("${llm.primary.api.url}")
    private String primaryApiUrl;
    @Value("${llm.primary.model.analysis}")
    private String primaryAnalysisModel;
    @Value("${llm.primary.model.suggestions}")
    private String primarySuggestionsModel;
    @Value("${llm.primary.model.random}")
    private String primaryRandomModel;

    // --- Secondary Provider Config ---
    @Value("${llm.secondary.provider}")
    private String secondaryProvider;
    @Value("${llm.secondary.api.key}")
    private String secondaryApiKey;
    @Value("${llm.secondary.api.url}")
    private String secondaryApiUrl;
    @Value("${llm.secondary.model.analysis}")
    private String secondaryAnalysisModel;
    @Value("${llm.secondary.model.suggestions}")
    private String secondarySuggestionsModel;
    @Value("${llm.secondary.model.random}")
    private String secondaryRandomModel;

    @Cacheable("analysis")
    public ProblemAnalysis analyzeTopic(String topic, List<Article> articles) {
        String consolidatedContent = articles.stream()
                .map(article -> "Title: " + article.title() + "\nDescription: " + article.description())
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                Act as an impartial policy analyst. Based on the following collection of news headlines and descriptions from the past week on the topic of '%s',
                synthesize the information to produce a concise analysis.

                Your response must be ONLY a valid JSON object with the following schema:
                {
                  "topic": "A 3-5 word title for the topic.",
                  "summary": "A one-paragraph summary of the key events or discussions.",
                  "aggregatedProblem": "The single most significant, underlying problem identified from these articles.",
                  "solutionProposal": "A creative and plausible solution to the aggregated problem.",
                  "proposingViewpoint": "Describe the perspective or potential bias of those who would support this news trend (e.g., government, a specific industry).",
                  "opposingViewpoint": "Describe the perspective or potential bias of those who would oppose or be critical of this trend.",
                  "historicalPerspective": "A brief historical parallel or context for this issue.",
                  "motivationalProverb": "An insightful proverb or quote related to the problem or solution."
                }

                Consolidated News Content:
                ---
                %s
                """
                .formatted(topic, consolidatedContent);

        try {
            String jsonResponseText = callLlmApi(prompt, primaryApiUrl, primaryAnalysisModel, primaryApiKey,
                    "application/json");
            return objectMapper.readValue(jsonResponseText, ProblemAnalysis.class);
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
            logger.warn("Primary provider (Gemini) unavailable ({}). Failing over to secondary (OpenRouter).",
                    e.getClass().getSimpleName());
            try {
                String jsonResponseText = callLlmApi(prompt, secondaryApiUrl, secondaryAnalysisModel, secondaryApiKey,
                        "application/json");
                return objectMapper.readValue(jsonResponseText, ProblemAnalysis.class);
            } catch (Exception ex) {
                logger.error("Secondary provider also failed.", ex);
                throw new RuntimeException("Both LLM providers failed for analysis", ex);
            }
        } catch (Exception e) {
            logger.error("Error during LLM analysis", e);
            throw new RuntimeException("LLM analysis failed", e);
        }
    }

    @Cacheable("topicSuggestions")
    public String getTopicSuggestions() {
        String prompt = "List 8 current and globally relevant news topics suitable for deep analysis. The topics should be 2-4 words long. Respond ONLY with a valid JSON array of strings. Example: [\"Global AI Regulation\", \"Future of Urban Mobility\"]";
        try {
            return callLlmApi(prompt, primaryApiUrl, primarySuggestionsModel, primaryApiKey, "application/json");
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
            logger.warn("Primary provider (Gemini) unavailable ({}). Failing over to secondary (OpenRouter).",
                    e.getClass().getSimpleName());
            try {
                return callLlmApi(prompt, secondaryApiUrl, secondarySuggestionsModel, secondaryApiKey,
                        "application/json");
            } catch (Exception ex) {
                logger.error("Secondary provider also failed for suggestions.", ex);
                return "[]";
            }
        } catch (Exception e) {
            logger.error("Error fetching topic suggestions", e);
            return "[]";
        }
    }

    // @Cacheable("randomTopic")
    public String getRandomSingleTopic() {
        String prompt = "Generate a single, interesting, and globally relevant news topic suitable for deep analysis. The topic should be 3-5 words long. Respond ONLY with the topic as a single plain text string, without quotes or any other formatting.";
        try {
            return callLlmApi(prompt, primaryApiUrl, primaryRandomModel, primaryApiKey, "text/plain");
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
            logger.warn("Primary provider (Gemini) unavailable ({}). Failing over to secondary (OpenRouter).",
                    e.getClass().getSimpleName());
            try {
                return callLlmApi(prompt, secondaryApiUrl, secondaryRandomModel, secondaryApiKey, "text/plain");
            } catch (Exception ex) {
                logger.error("Secondary provider also failed for random topic.", ex);
                return "Global economic trends";
            }
        } catch (Exception e) {
            logger.error("Error fetching random topic", e);
            return "Global economic trends";
        }
    }

    private String callLlmApi(String prompt, String baseUrl, String model, String apiKey, String mimeType)
            throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Object requestBody;
        String fullUrl;

        // Differentiate between Gemini and OpenAI-compatible (OpenRouter) API
        // structures
        if (baseUrl.contains("openrouter")) {
            headers.set("Authorization", "Bearer " + apiKey);
            requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));
            fullUrl = baseUrl;
        } else { // Gemini
            var parts = List.of(new GeminiDto.Part(prompt));
            var contents = List.of(new GeminiDto.Content(parts));
            var generationConfig = new GeminiDto.GenerationConfig(mimeType);
            requestBody = new GeminiDto.GeminiRequest(contents, generationConfig);
            fullUrl = baseUrl + model + ":generateContent?key=" + apiKey;
        }

        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        String responseBody = restTemplate.postForObject(fullUrl, entity, String.class);

        String jsonText;
        // Parse the response differently based on the provider
        if (baseUrl.contains("openrouter")) {
            jsonText = objectMapper.readTree(responseBody).at("/choices/0/message/content").asText();
        } else { // Gemini
            jsonText = objectMapper.readTree(responseBody).at("/candidates/0/content/parts/0/text").asText();
        }

        // SANITIZATION :: Trim whitespace and remove potential markdown code blocks
        String sanitizedJson = jsonText.trim();
        if (sanitizedJson.startsWith("```json")) {
            sanitizedJson = sanitizedJson.substring(7);
            if (sanitizedJson.endsWith("```")) {
                sanitizedJson = sanitizedJson.substring(0, sanitizedJson.length() - 3);
            }
        } else if (sanitizedJson.startsWith("`") && sanitizedJson.endsWith("`")) {
            sanitizedJson = sanitizedJson.substring(1, sanitizedJson.length() - 1);
        }

        return sanitizedJson.trim(); // Return the cleaned JSON string
    }

}