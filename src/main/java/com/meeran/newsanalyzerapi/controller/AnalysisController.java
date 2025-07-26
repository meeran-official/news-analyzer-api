package com.meeran.newsanalyzerapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meeran.newsanalyzerapi.dto.AnalysisDto.ProblemAnalysis;
import com.meeran.newsanalyzerapi.dto.NewsApiResponse;
import com.meeran.newsanalyzerapi.service.AnalysisService;
import com.meeran.newsanalyzerapi.service.NewsService;

@RestController
@RequestMapping("/api/v1/analyze")
public class AnalysisController {
    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final NewsService newsService;
    private final AnalysisService analysisService;

    public AnalysisController(NewsService newsService, AnalysisService analysisService) {
        this.newsService = newsService;
        this.analysisService = analysisService;
    }

    @GetMapping
    public ResponseEntity<?> getAnalysisByTopic(@RequestParam String topic) {
        log.info("Received request to analyze topic: {}", topic);
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Topic cannot be empty.\"}");
        }
        NewsApiResponse newsResponse = newsService.fetchArticlesForTopic(topic);

        if (newsResponse != null && !newsResponse.articles().isEmpty()) {
            ProblemAnalysis analysis = analysisService.analyzeTopic(topic, newsResponse.articles());

            if (analysis != null) {
                return ResponseEntity.ok(analysis);
            }
        }
        // Failure: return 500 Internal Server Error with a clear message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"Failed to retrieve or analyze the topic: " + topic + "\"}");
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getTopicSuggestions() {

        String suggestionsJson = analysisService.getTopicSuggestions();
        return ResponseEntity.ok(suggestionsJson);
    }

    @GetMapping("/random-topic")
    public ResponseEntity<String> getRandomSingleTopic() {
        String randomTopic = analysisService.getRandomSingleTopic();
        return ResponseEntity.ok(randomTopic);
    }
}