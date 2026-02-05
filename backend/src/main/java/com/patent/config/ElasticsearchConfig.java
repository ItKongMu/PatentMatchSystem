package com.patent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import java.time.Duration;

/**
 * Elasticsearch 配置类
 * 
 * <p>继承 {@link ElasticsearchConfiguration} 来自定义 ES 连接配置。
 * Spring Data Elasticsearch 会自动创建以下 Bean：
 * <ul>
 *   <li>elasticsearchOperations - {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations} 实例</li>
 *   <li>elasticsearchClient - ES 客户端实例</li>
 * </ul>
 * 
 * @see <a href="https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/clients.html">Spring Data Elasticsearch Clients</a>
 */
@Slf4j
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.data.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Override
    public ClientConfiguration clientConfiguration() {
        // 解析 URI，移除协议前缀
        String hostAndPort = esUris
                .replace("http://", "")
                .replace("https://", "");
        
        log.info("初始化 Elasticsearch 连接: {}", hostAndPort);
        
        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .withConnectTimeout(Duration.ofSeconds(10))
                .withSocketTimeout(Duration.ofSeconds(30))
                .build();
    }
}
