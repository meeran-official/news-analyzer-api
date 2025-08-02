package com.meeran.newsanalyzerapi.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.meeran.newsanalyzerapi.dto.NewsApiResponse;

class NewsServiceTest {

    @Test
    void fetchArticlesForTopic_encodesTopicWithSpaces() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        NewsService service = new NewsService(restTemplate);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        when(restTemplate.getForObject(any(URI.class), eq(NewsApiResponse.class))).thenReturn(new NewsApiResponse());

        service.fetchArticlesForTopic("climate change");

        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(captor.capture(), eq(NewsApiResponse.class));
        URI calledUri = captor.getValue();

        assertTrue(calledUri.toString().contains("q=climate%20change"));
        assertTrue(calledUri.toString().contains("apiKey=test-key"));
    }

    @Test
    void fetchArticlesForTopic_encodesSpecialCharacters() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        NewsService service = new NewsService(restTemplate);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        when(restTemplate.getForObject(any(URI.class), eq(NewsApiResponse.class))).thenReturn(new NewsApiResponse());

        service.fetchArticlesForTopic("c# & kotlin");

        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(captor.capture(), eq(NewsApiResponse.class));
        URI calledUri = captor.getValue();

        String uri = calledUri.toString();
        assertTrue(uri.contains("q=c%23%20%26%20kotlin"));
    }
}
