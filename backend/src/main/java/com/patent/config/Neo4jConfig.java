package com.patent.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j 配置类
 * 1. 注册 neo4jTransactionManager bean，供 @Transactional("neo4jTransactionManager") 使用
 * 2. 应用启动时自动执行 neo4j-constraints.cypher 初始化约束与索引
 */
@Slf4j
@Configuration
public class Neo4jConfig {

    private final Driver driver;
    private final Neo4jClient neo4jClient;

    public Neo4jConfig(Driver driver, Neo4jClient neo4jClient) {
        this.driver = driver;
        this.neo4jClient = neo4jClient;
    }

    /**
     * 显式注册 Neo4j 事务管理器，bean 名称为 neo4jTransactionManager
     * GraphServiceImpl 中 @Transactional("neo4jTransactionManager") 依赖此 bean
     */
    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager() {
        return new Neo4jTransactionManager(driver);
    }

    /**
     * 应用启动后检查并初始化 Neo4j 约束与索引
     * 通过查询已有约束数量判断是否首次初始化，避免重复执行
     */
    @PostConstruct
    public void initNeo4jConstraints() {
        try {
            ClassPathResource resource = new ClassPathResource("db/neo4j-init.cypher");
            if (!resource.exists()) {
                log.warn("Neo4j 约束脚本不存在: db/neo4j-init.cypher，跳过初始化");
                return;
            }

            // 检查是否已初始化：查询名为 patent_pubno_unique 的约束是否存在
            try (Session session = driver.session()) {
                boolean alreadyInitialized = session.run(
                    "SHOW CONSTRAINTS WHERE name = 'patent_pubno_unique'"
                ).hasNext();

                if (alreadyInitialized) {
                    log.info("Neo4j 约束已存在，跳过初始化");
                    return;
                }
            }

            List<String> statements = parseCypherStatements(resource);
            if (statements.isEmpty()) {
                return;
            }

            try (Session session = driver.session()) {
                for (String stmt : statements) {
                    try {
                        session.run(stmt).consume();
                        log.debug("Neo4j 执行成功: {}", stmt.substring(0, Math.min(80, stmt.length())));
                    } catch (Exception e) {
                        log.warn("Neo4j 语句执行失败（可忽略）: {} | 原因: {}", stmt.substring(0, Math.min(80, stmt.length())), e.getMessage());
                    }
                }
            }
            log.info("Neo4j 约束与索引初始化完成，共执行 {} 条语句", statements.size());
        } catch (Exception e) {
            log.error("Neo4j 约束初始化异常（不影响启动）: {}", e.getMessage());
        }
    }

    /**
     * 解析 Cypher 文件，按分号分割语句，过滤注释和空行
     */
    private List<String> parseCypherStatements(ClassPathResource resource) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // 跳过注释行和空行
                if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                    continue;
                }
                current.append(trimmed).append(" ");
                // 以分号结尾则为一条完整语句
                if (trimmed.endsWith(";")) {
                    String stmt = current.toString().trim();
                    // 去掉末尾分号（Neo4j Driver 不需要）
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current.setLength(0);
                }
            }
            // 处理没有分号结尾的最后一条语句
            String remaining = current.toString().trim();
            if (!remaining.isEmpty()) {
                statements.add(remaining);
            }
        }
        return statements;
    }
}
