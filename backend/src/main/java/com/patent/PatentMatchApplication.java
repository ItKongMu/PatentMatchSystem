package com.patent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 专利技术匹配系统启动类
 * 基于大语言模型进行实体和领域增强的专利技术匹配系统
 *
 * @author patent-match-system
 */
@SpringBootApplication
@MapperScan("com.patent.mapper")
@EnableAsync
public class PatentMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatentMatchApplication.class, args);
        System.out.println("==================================================");
        System.out.println("   专利技术匹配系统启动成功！");
        System.out.println("   API文档: http://localhost:8080/doc.html");
        System.out.println("==================================================");
    }
}
