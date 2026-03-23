package com.patent.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.HttpURLConnection;
import java.time.Duration;

/**
 * RestClient配置
 * 用于配置Spring AI的HTTP请求超时
 *
 * <p>修复说明（Connection reset 问题）：
 * <ul>
 *   <li>根因：JDK HttpClient 默认复用 Keep-Alive 连接；
 *       当阿里云 DashScope 服务端空闲超时后主动关闭该连接，
 *       客户端复用时收到 Connection reset。</li>
 *   <li>方案：切换为 SimpleClientHttpRequestFactory（基于 HttpURLConnection），
 *       并设置 "Connection: close" 请求头，禁用 Keep-Alive 连接复用，
 *       每次请求建立新连接，彻底避免 stale connection 问题。</li>
 *   <li>代价：每次请求多一次 TCP 握手（Embedding 请求本身耗时 < 200ms，影响极小）。</li>
 * </ul>
 * </p>
 */
@Configuration
public class RestClientConfig {

    /**
     * 配置 RestClient.Builder：
     * <ul>
     *   <li>使用 SimpleClientHttpRequestFactory（HttpURLConnection），避免 JDK HttpClient 的连接池问题</li>
     *   <li>禁用 HTTP Keep-Alive，防止 stale connection 导致的 Connection reset</li>
     *   <li>连接超时 60s，读取超时 180s（LLM 响应可能较慢）</li>
     * </ul>
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        // 使用 SimpleClientHttpRequestFactory，底层为 HttpURLConnection
        // 相比 JDK HttpClient，它不维护连接池，可通过 setChunkedTransferEncoding(false)
        // 配合 "Connection: close" 请求头完全禁用 Keep-Alive
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(60).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(180).toMillis());

        return RestClient.builder()
                .requestFactory(factory)
                // 全局添加 "Connection: close" 请求头，强制每次请求后关闭连接
                // 这是解决 DashScope（阿里云）等服务端主动断开 Keep-Alive 连接导致
                // "Connection reset" 的最直接方案
                .defaultHeader("Connection", "close");
    }
}
