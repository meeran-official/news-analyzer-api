package com.meeran.newsanalyzerapi.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.meeran.newsanalyzerapi.dto.Article;
import com.meeran.newsanalyzerapi.dto.MediastackDto;
import com.meeran.newsanalyzerapi.dto.NewsApiResponse;

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
            logger.warn("Primary news provider failed. Failing over to secondary provider (Mediastack).", e);
            return fetchFromMediastack(topic);
        } catch (Exception e) {
            logger.error("Primary news provider failed. Attempting fallback.", e);
            return fetchFromMediastack(topic);
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

    private NewsApiResponse fetchFromMediastack(String topic) {
        String url = UriComponentsBuilder.fromHttpUrl(secondaryApiUrl)
                .queryParam("access_key", secondaryApiKey)
                .queryParam("keywords", topic)
                .queryParam("languages", "en")
                .queryParam("limit", 20)
                .toUriString();
        
        logger.info("Calling Mediastack with URL: {}", url);

        try {
            MediastackDto.Response response = restTemplate.getForObject(url, MediastackDto.Response.class);
            if (response != null && response.data() != null && !response.data().isEmpty()) {
                List<Article> mappedArticles = response.data().stream()
                        .map(dtoArticle -> new Article(dtoArticle.title(), dtoArticle.description(), dtoArticle.url()))
                        .collect(Collectors.toList());
                return new NewsApiResponse("ok", mappedArticles.size(), mappedArticles);
            }
        } catch (Exception e) {
            logger.error("Tertiary news provider (Mediastack) also failed.", e);
        }
        return new NewsApiResponse("error", 0, Collections.emptyList());
    }
}