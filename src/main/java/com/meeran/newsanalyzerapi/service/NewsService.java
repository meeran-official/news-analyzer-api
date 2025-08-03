package com.meeran.newsanalyzerapi.service;

import com.meeran.newsanalyzerapi.dto.Article;
import com.meeran.newsanalyzerapi.dto.NewsApiResponse;
import com.meeran.newsanalyzerapi.dto.NewsDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    @Value("${news.primary.api.key}")
    private String primaryApiKey;
    @Value("${news.primary.api.url}")
    private String primaryApiUrl;

    @Value("${news.secondary.api.key}")
    private String secondaryApiKey;
    @Value("${news.secondary.api.url}")
    private String secondaryApiUrl;

    private final RestTemplate restTemplate;

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable("newsArticles")
    public NewsApiResponse fetchArticlesForTopic(String topic) {
        try {
            logger.info("Attempting to fetch articles from primary provider (NewsAPI.org)");
            return fetchFromNewsAPI(topic);
        } catch (HttpClientErrorException.TooManyRequests e) {
            logger.warn("Primary news provider rate limited. Failing over to secondary provider (NewsData.io).");
            return fetchFromNewsData(topic);
        } catch (Exception e) {
            logger.error("Primary news provider failed. Attempting fallback.", e);
            return fetchFromNewsData(topic);
        }
    }

    private NewsApiResponse fetchFromNewsAPI(String topic) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        String url = UriComponentsBuilder.fromHttpUrl(primaryApiUrl)
            .queryParam("q", topic)
            .queryParam("from", fromDate.format(formatter))
            .queryParam("to", toDate.format(formatter))
            .queryParam("sortBy", "popularity")
            .queryParam("language", "en")
            .queryParam("pageSize", 20)
            .queryParam("apiKey", primaryApiKey)
            .toUriString();

        return restTemplate.getForObject(url, NewsApiResponse.class);
    }

    private NewsApiResponse fetchFromNewsData(String topic) {
        String url = UriComponentsBuilder.fromHttpUrl(secondaryApiUrl)
            .queryParam("q", topic)
            .queryParam("language", "en")
            .queryParam("apikey", secondaryApiKey)
            .toUriString();

        try {
            NewsDataDto.Response response = restTemplate.getForObject(url, NewsDataDto.Response.class);
            if (response != null && "success".equals(response.status())) {
                // Map the NewsData.io DTOs to our application's standard DTOs
                List<Article> mappedArticles = response.results().stream()
                    .map(dtoArticle -> new Article(dtoArticle.title(), dtoArticle.description(), dtoArticle.link()))
                    .collect(Collectors.toList());
                return new NewsApiResponse("ok", response.totalResults(), mappedArticles);
            }
        } catch (Exception e) {
            logger.error("Secondary news provider (NewsData.io) also failed.", e);
        }
        // Return an empty response if all providers fail
        return new NewsApiResponse("error", 0, Collections.emptyList());
    }
}