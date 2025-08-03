package com.meeran.newsanalyzerapi.controller;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.meeran.newsanalyzerapi.dto.AnalysisDto.ProblemAnalysis;
import com.meeran.newsanalyzerapi.dto.NewsApiResponse;
import com.meeran.newsanalyzerapi.service.AnalysisService;
import com.meeran.newsanalyzerapi.service.NewsService;

@RestController
@RequestMapping("/api/v1/analyze")
public class AnalysisController {
    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final NewsService newsService;
    private final AnalysisService analysisService;

    public AnalysisController(NewsService newsService, AnalysisService analysisService) {
        this.newsService = newsService;
        this.analysisService = analysisService;
    }

    @GetMapping
    public ResponseEntity<?> getAnalysisByTopic(
            @RequestParam String topic,
            @RequestParam(defaultValue = "english") String language) {
        log.info("Received request to analyze topic: {} in language: {}", topic, language);
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Topic cannot be empty.\"}");
        }

        try {
            NewsApiResponse newsResponse = newsService.fetchArticlesForTopic(topic);

            // Handle case where no articles were found
            if (newsResponse == null || newsResponse.articles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"No recent news articles found for the topic: " + topic
                                + ". Please try another topic.\"}");
            }

            // Proceed with analysis if articles were found
            ProblemAnalysis analysis = analysisService.analyzeTopic(topic, newsResponse.articles(), language);

            if (analysis != null) {
                return ResponseEntity.ok(analysis);
            }

            // Fallback for analysis failure
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to analyze the topic after retrieving articles: " + topic + "\"}");

        } catch (RuntimeException e) {
            log.error("Analysis failed for topic: {}", topic, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getTopicSuggestions() {
        String suggestionsJson = analysisService.getTopicSuggestions();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<String> suggestions = mapper.readValue(suggestionsJson, new TypeReference<List<String>>() {
            });
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Failed to parse topic suggestions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/random-topic")
    public ResponseEntity<String> getRandomSingleTopic() {
        String randomTopic = analysisService.getRandomSingleTopic();
        return ResponseEntity.ok(randomTopic);
    }
}