package com.patent;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Elasticsearch 连接测试工具
 * 直接运行 main 方法进行诊断
 */
public class ElasticsearchConnectionTest {

    // ============ 修改这里的配置 ============
    private static final String ES_HOST = "192.168.65.131";
    private static final int ES_PORT = 9200;
    // ========================================

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("       Elasticsearch 连接诊断工具");
        System.out.println("====================================================");
        System.out.println("目标地址: " + ES_HOST + ":" + ES_PORT);
        System.out.println("----------------------------------------------------\n");

        // 测试1: 基础网络连通性
        boolean networkOk = testNetworkConnectivity();
        
        // 测试2: HTTP 连接
        boolean httpOk = testHttpConnection();
        
        // 测试3: ES 客户端连接
        boolean clientOk = testElasticsearchClient();
        
        // 测试4: 索引操作
        if (clientOk) {
            testIndexOperations();
        }

        // 总结
        System.out.println("\n====================================================");
        System.out.println("                    诊断总结");
        System.out.println("====================================================");
        System.out.println("网络连通性: " + (networkOk ? "✓ 正常" : "✗ 失败"));
        System.out.println("HTTP 连接:  " + (httpOk ? "✓ 正常" : "✗ 失败"));
        System.out.println("ES 客户端:  " + (clientOk ? "✓ 正常" : "✗ 失败"));
        
        if (!networkOk) {
            System.out.println("\n[建议] 网络不通，请检查:");
            System.out.println("  1. ES 服务是否已启动");
            System.out.println("  2. 防火墙是否开放 " + ES_PORT + " 端口");
            System.out.println("  3. ES 配置 network.host 是否允许远程访问");
            System.out.println("  4. 虚拟机/容器网络配置是否正确");
        } else if (!httpOk) {
            System.out.println("\n[建议] HTTP 连接失败，请检查:");
            System.out.println("  1. ES 服务是否正常运行（未崩溃）");
            System.out.println("  2. ES 是否配置了 HTTPS（需要使用 https://）");
            System.out.println("  3. ES 是否配置了认证（需要用户名密码）");
        } else if (!clientOk) {
            System.out.println("\n[建议] ES 客户端连接失败，请检查:");
            System.out.println("  1. ES 版本是否兼容（推荐 ES 8.x）");
            System.out.println("  2. 客户端库版本是否匹配");
        }
        
        System.out.println("====================================================");
    }

    /**
     * 测试1: 基础网络连通性（TCP Socket）
     */
    private static boolean testNetworkConnectivity() {
        System.out.println("[测试1] 检查网络连通性 (TCP Socket)...");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ES_HOST, ES_PORT), 5000);
            System.out.println("  ✓ 网络连通，可以访问 " + ES_HOST + ":" + ES_PORT);
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ 网络不通: " + e.getMessage());
            return false;
        }
    }

    /**
     * 测试2: HTTP 连接
     */
    private static boolean testHttpConnection() {
        System.out.println("\n[测试2] 检查 HTTP 连接...");
        try {
            URL url = new URL("http://" + ES_HOST + ":" + ES_PORT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            System.out.println("  HTTP 响应码: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                System.out.println("  ✓ HTTP 连接成功");
                System.out.println("  ES 响应: " + response.toString().substring(0, Math.min(200, response.length())) + "...");
                return true;
            } else if (responseCode == 401) {
                System.out.println("  ✗ 需要认证 (HTTP 401)，ES 可能配置了安全认证");
                return false;
            } else {
                System.out.println("  ✗ HTTP 响应异常: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.out.println("  ✗ HTTP 连接失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 测试3: ES 客户端连接
     */
    private static boolean testElasticsearchClient() {
        System.out.println("\n[测试3] 检查 Elasticsearch 客户端连接...");
        
        RestClient restClient = null;
        ElasticsearchTransport transport = null;
        
        try {
            // 创建 RestClient，添加 X-Elastic-Product header 以兼容 ES 7.x
            restClient = RestClient.builder(new HttpHost(ES_HOST, ES_PORT, "http"))
                    .setDefaultHeaders(new org.apache.http.Header[]{
                            new org.apache.http.message.BasicHeader("X-Elastic-Product", "Elasticsearch")
                    })
                    .build();
            
            // 创建 Transport
            transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            
            // 创建 ElasticsearchClient
            ElasticsearchClient client = new ElasticsearchClient(transport);
            
            // 获取集群信息
            InfoResponse info = client.info();
            
            System.out.println("  ✓ ES 客户端连接成功");
            System.out.println("  集群名称: " + info.clusterName());
            System.out.println("  节点名称: " + info.name());
            System.out.println("  ES 版本:  " + info.version().number());
            System.out.println("  Lucene 版本: " + info.version().luceneVersion());
            
            // 检查版本兼容性警告
            String version = info.version().number();
            if (version.startsWith("7.")) {
                System.out.println("\n  ⚠ 警告: 检测到 ES 7.x 版本，Spring Data Elasticsearch 5.x 推荐使用 ES 8.x");
                System.out.println("  当前已配置兼容模式，功能可正常使用");
            }
            
            return true;
            
        } catch (Exception e) {
            System.out.println("  ✗ ES 客户端连接失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (transport != null) transport.close();
                if (restClient != null) restClient.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 测试4: 索引操作
     */
    private static void testIndexOperations() {
        System.out.println("\n[测试4] 检查索引操作...");
        
        RestClient restClient = null;
        ElasticsearchTransport transport = null;
        
        try {
            restClient = RestClient.builder(new HttpHost(ES_HOST, ES_PORT, "http"))
                    .setDefaultHeaders(new org.apache.http.Header[]{
                            new org.apache.http.message.BasicHeader("X-Elastic-Product", "Elasticsearch")
                    })
                    .build();
            transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            ElasticsearchClient client = new ElasticsearchClient(transport);
            
            // 检查 patent_index 是否存在
            String indexName = "patent_index";
            boolean indexExists = client.indices().exists(e -> e.index(indexName)).value();
            
            if (indexExists) {
                System.out.println("  ✓ 索引 '" + indexName + "' 已存在");
                
                // 获取索引文档数量
                var countResponse = client.count(c -> c.index(indexName));
                System.out.println("  文档数量: " + countResponse.count());
            } else {
                System.out.println("  ! 索引 '" + indexName + "' 不存在");
                System.out.println("  [建议] 请先创建索引，或通过 /api/search/init-index 接口初始化");
            }
            
        } catch (Exception e) {
            System.out.println("  ✗ 索引操作失败: " + e.getMessage());
        } finally {
            try {
                if (transport != null) transport.close();
                if (restClient != null) restClient.close();
            } catch (Exception ignored) {}
        }
    }
}
