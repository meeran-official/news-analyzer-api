package com.meeran.newsanalyzerapi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    // --- Third Provider Config ---
    @Value("${llm.third.provider}")
    private String thirdProvider;
    @Value("${llm.third.api.key}")
    private String thirdApiKey;
    @Value("${llm.third.api.url}")
    private String thirdApiUrl;
    @Value("${llm.third.model.analysis}")
    private String thirdAnalysisModel;
    @Value("${llm.third.model.suggestions}")
    private String thirdSuggestionsModel;
    @Value("${llm.third.model.random}")    
    private String thirdRandomModel;

    // Backwards compatibility method
    @Cacheable("analysis")
    public ProblemAnalysis analyzeTopic(String topic, List<Article> articles) {
        return analyzeTopic(topic, articles, "english");
    }
    
    @Cacheable("analysis")
    public ProblemAnalysis analyzeTopic(String topic, List<Article> articles, String language) {
        String consolidatedContent = articles.stream()
                .map(article -> "Title: " + article.title() + "\nDescription: " + article.description())
                .collect(Collectors.joining("\n\n"));

        boolean isTamil = "tamil".equalsIgnoreCase(language);
        
        String prompt = isTamil ? 
                """
                ஒரு நடுநிலையான கொள்கை ஆய்வாளராக செயல்படுங்கள். '%s' என்ற தலைப்பில் கடந்த வாரத்தின் செய்தி தலைப்புகள் மற்றும் விளக்கங்களின் அடிப்படையில்,
                தகவல்களை ஒருங்கிணைத்து சுருக்கமான பகுப்பாய்வை உருவாக்குங்கள்.

                உங்கள் பதில் பின்வரும் schema உடன் கூடிய JSON object மட்டுமே இருக்க வேண்டும்:
                {
                  "topic": "தலைப்புக்கான 3-5 சொற்கள் தமிழில்.",
                  "summary": "முக்கிய நிகழ்வுகள் அல்லது விவாதங்களின் ஒரு பத்தி சுருக்கம் தமிழில்.",
                  "aggregatedProblem": "இந்த கட்டுரைகளில் இருந்து அடையாளம் காணப்பட்ட மிக முக்கியமான அடிப்படை பிரச்சினை தமிழில்.",
                  "solutionProposal": "அடிப்படை பிரச்சனைக்கு ஒரு ஆக்கபூர்வமான மற்றும் நம்பகமான தீர்வு தமிழில்.",
                  "proposingViewpoint": "இந்த செய்தி போக்கை ஆதரிப்பவர்களின் கோணம் அல்லது சாத்தியமான சார்பு தமிழில்.",
                  "opposingViewpoint": "இந்த போக்கை எதிர்க்கும் அல்லது விமர்சிப்பவர்களின் கோணம் தமிழில்.",
                  "historicalPerspective": "இந்த பிரச்சினைக்கான சுருக்கமான வரலாற்று இணை அல்லது சூழல் தமிழில்.",
                  "motivationalProverb": "பிரச்சினை அல்லது தீர்வுடன் தொடர்புடைய ஒரு ஆழமான பழமொழி அல்லது மேற்கோள் தமிழில்."
                }

                ஒருங்கிணைக்கப்பட்ட செய்தி உள்ளடக்கம்:
                ---
                %s
                """ :
                """
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
                """;
        
        prompt = prompt.formatted(topic, consolidatedContent);

        try {
            String jsonResponseText = callLlmApi(prompt, primaryApiUrl, primaryAnalysisModel, primaryApiKey,
                    "application/json");
            try {
                return objectMapper.readValue(jsonResponseText, ProblemAnalysis.class);
            } catch (Exception parseEx) {
                logger.warn("Primary provider (Gemini) returned unparsable JSON. Attempting secondary provider.");
                // Try secondary provider immediately
                try {
                    String secondaryJson = callLlmApi(prompt, secondaryApiUrl, secondaryAnalysisModel, secondaryApiKey,
                            "application/json");
                    return objectMapper.readValue(secondaryJson, ProblemAnalysis.class);
                } catch (Exception secondaryEx) {
                    logger.warn("Secondary provider also failed to parse JSON. Attempting third provider (OpenRouter).");
                    // Try third provider
                    try {
                        String thirdJson = callLlmApi(prompt, thirdApiUrl, thirdAnalysisModel, thirdApiKey,
                                "application/json");
                        return objectMapper.readValue(thirdJson, ProblemAnalysis.class);
                    } catch (Exception thirdEx) {
                        logger.error("All three LLM providers failed to parse JSON.", thirdEx);
                        throw new RuntimeException("All LLM providers returned invalid responses.", thirdEx);
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            logger.warn("Primary provider (Gemini) failed with status {}: {}. Failing over to secondary provider.",
                    e.getStatusCode(), e.getResponseBodyAsString());
            try {
                String jsonResponseText = callLlmApi(prompt, secondaryApiUrl, secondaryAnalysisModel, secondaryApiKey,
                        "application/json");
                return objectMapper.readValue(jsonResponseText, ProblemAnalysis.class);
            } catch (Exception secondaryEx) {
                logger.warn("Secondary provider also failed. Failing over to third provider (OpenRouter).");
                try {
                    String thirdJson = callLlmApi(prompt, thirdApiUrl, thirdAnalysisModel, thirdApiKey,
                            "application/json");
                    return objectMapper.readValue(thirdJson, ProblemAnalysis.class);
                } catch (Exception thirdEx) {
                    logger.error("All three providers failed.", thirdEx);
                    throw new RuntimeException("Content analysis unavailable. This topic may be restricted by our AI providers or experiencing high demand. Please try a different topic.", thirdEx);
                }
            }
        } catch (HttpServerErrorException e) {
            logger.warn("Primary provider (Gemini) server error ({}). Failing over to secondary provider.",
                    e.getClass().getSimpleName());
            try {
                String jsonResponseText = callLlmApi(prompt, secondaryApiUrl, secondaryAnalysisModel, secondaryApiKey,
                        "application/json");
                return objectMapper.readValue(jsonResponseText, ProblemAnalysis.class);
            } catch (Exception secondaryEx) {
                logger.warn("Secondary provider also failed. Failing over to third provider (OpenRouter).");
                try {
                    String thirdJson = callLlmApi(prompt, thirdApiUrl, thirdAnalysisModel, thirdApiKey,
                            "application/json");
                    return objectMapper.readValue(thirdJson, ProblemAnalysis.class);
                } catch (Exception thirdEx) {
                    logger.error("All three providers failed.", thirdEx);
                    throw new RuntimeException("Analysis service temporarily unavailable. Please try again in a few moments.", thirdEx);
                }
            }
        } catch (Exception e) {
            logger.error("Error during LLM analysis", e);
            throw new RuntimeException("Unable to analyze this topic. Please try a different topic or try again later.", e);
        }
    }

    @Cacheable("topicSuggestions")
    public String getTopicSuggestions() {
        String prompt = "List 8 current and globally relevant news topics suitable for deep analysis. The topics should be 2-4 words long. Respond ONLY with a valid JSON array of strings. Example: [\"Global AI Regulation\", \"Future of Urban Mobility\"]";
        try {
            return callLlmApi(prompt, primaryApiUrl, primarySuggestionsModel, primaryApiKey, "application/json");
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
            logger.warn("Primary provider (Gemini) unavailable ({}). Failing over to secondary provider.",
                    e.getClass().getSimpleName());
            try {
                return callLlmApi(prompt, secondaryApiUrl, secondarySuggestionsModel, secondaryApiKey,
                        "application/json");
            } catch (Exception secondaryEx) {
                logger.warn("Secondary provider also failed for suggestions. Failing over to third provider (OpenRouter).");
                try {
                    return callLlmApi(prompt, thirdApiUrl, thirdSuggestionsModel, thirdApiKey,
                            "application/json");
                } catch (Exception thirdEx) {
                    logger.error("All three providers failed for suggestions.", thirdEx);
                    return "[]";
                }
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
            logger.warn("Primary provider (Gemini) unavailable ({}). Failing over to secondary provider.",
                    e.getClass().getSimpleName());
            try {
                return callLlmApi(prompt, secondaryApiUrl, secondaryRandomModel, secondaryApiKey, "text/plain");
            } catch (Exception secondaryEx) {
                logger.warn("Secondary provider also failed for random topic. Failing over to third provider (OpenRouter).");
                try {
                    return callLlmApi(prompt, thirdApiUrl, thirdRandomModel, thirdApiKey, "text/plain");
                } catch (Exception thirdEx) {
                    logger.error("All three providers failed for random topic.", thirdEx);
                    return "Global economic trends";
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching random topic", e);
            return "Global economic trends";
        }
    }

    private String callLlmApi(String prompt, String baseUrl, String model, String apiKey, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Ensure timeouts for external provider calls
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate.setRequestFactory(requestFactory);

        Object requestBody;
        String fullUrl;

        // Differentiate between Gemini and OpenAI-compatible (OpenRouter) API structures
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

        int attempts = 0;
        while (attempts < 2) {
            try {
                var response = restTemplate.postForEntity(fullUrl, entity, String.class);
                logger.info("LLM API response status={} body={}", response.getStatusCode().value(), response.getBody());

                String responseBody = response.getBody();
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
            } catch (ResourceAccessException e) {
                attempts++;
                logger.warn("LLM API request attempt {} failed due to resource access: {}", attempts, e.getMessage());
            } catch (IOException e) {
                attempts++;
                logger.warn("LLM API request attempt {} failed due to IO: {}", attempts, e.getMessage());
            }
        }

        logger.error("LLM API request failed after {} attempts; returning fallback response", attempts);
        return "[]";
    }

}