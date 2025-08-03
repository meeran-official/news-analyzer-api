package com.meeran.newsanalyzerapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Configure the RestTemplate to use the Apache HttpClient 5 request factory
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    }
}