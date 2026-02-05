package com.patent.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient配置
 * 用于配置Spring AI的HTTP请求超时
 */
@Configuration
public class RestClientConfig {

    /**
     * 配置RestClient.Builder，增加超时时间
     * Spring AI会使用这个Builder来创建HTTP客户端
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(60))
                .withReadTimeout(Duration.ofSeconds(180)); // LLM响应可能较慢，设置3分钟
        
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        
        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
