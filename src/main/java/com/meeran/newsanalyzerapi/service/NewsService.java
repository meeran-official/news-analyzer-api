package com.meeran.newsanalyzerapi.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.meeran.newsanalyzerapi.dto.NewsApiResponse;

@Service
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    // Injects the API key from application.properties
    @Value("${news.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String NEWS_API_URL = "https://newsapi.org/v2/everything";
    
    public NewsApiResponse fetchArticlesForTopic(String topic) {
        // Get today's date and the date for 7 days ago
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        String url = NEWS_API_URL
            + "?q=" + topic
            + "&from=" + fromDate.format(formatter)
            + "&to=" + toDate.format(formatter)
            + "&sortBy=popularity"
            + "&language=en"
            + "&pageSize=20" // Fetch up to 20 articles
            + "&apiKey=" + apiKey;

        logger.info("Fetching news from URL: {}", url);


        try {
            return restTemplate.getForObject(url, NewsApiResponse.class);
        } catch (HttpClientErrorException e) {
            
            // This will catch errors like 401 (unauthorized), 400 (bad request), etc.
            
            logger.error("API Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null; // Return null to indicate failure
        } catch (Exception e) {
            
            // This will catch other errors like network issues
            
            logger.error("An unexpected error occurred", e);
            return null;
        }
    }
}