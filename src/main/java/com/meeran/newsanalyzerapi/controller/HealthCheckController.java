package com.meeran.newsanalyzerapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meeran.newsanalyzerapi.service.NewsService;

@RestController
@RequestMapping("/api/v1/test-news")
public class HealthCheckController {
    
    private final NewsService newsService;

    // Spring injects the NewsService instance here
    public HealthCheckController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public String getFirstArticleTitle() {
        // NewsApiResponse response = newsService.fetchArticlesForTopic();
        // if (response != null && response.articles() != null && !response.articles().isEmpty()) {
        //     // Return the title of the very first article
        //     return "Latest Article Title: " + response.articles().get(0).title();
        // }
        return "No articles found or API error.";
    }
}