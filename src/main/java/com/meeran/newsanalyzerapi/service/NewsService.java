package com.meeran.newsanalyzerapi.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(NEWS_API_URL)
            .queryParam("q", topic)
            .queryParam("from", fromDate.format(formatter))
            .queryParam("to", toDate.format(formatter))
            .queryParam("sortBy", "popularity")
            .queryParam("language", "en")
            .queryParam("pageSize", 20);

        String sanitizedUrl = builder.toUriString();
        String url = builder.queryParam("apiKey", apiKey).toUriString();

        logger.info("Fetching news from URL: {}", sanitizedUrl);


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