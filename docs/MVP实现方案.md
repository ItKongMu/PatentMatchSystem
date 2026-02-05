# 基于大语言模型进行实体和领域增强的专利技术匹配系统

## MVP（最小可行性产品）实现方案

---

## 目录

1. [MVP核心价值与功能范围](#1-mvp核心价值与功能范围)
2. [系统架构设计](#2-系统架构设计)
3. [技术选型与版本](#3-技术选型与版本)
4. [数据库设计](#4-数据库设计)
5. [核心功能实现步骤](#5-核心功能实现步骤)
6. [API接口设计](#6-api接口设计)
7. [配置文件](#7-配置文件)
8. [部署方案](#8-部署方案)
9. [开发计划](#9-开发计划)

---

## 1. MVP核心价值与功能范围

### 1.1 核心价值验证目标

**验证命题**：通过大语言模型抽取专利中的实体与层次化领域信息，并用于提升专利技术匹配效果

### 1.2 MVP功能清单

| 功能模块 | MVP功能项      | 优先级 | 说明                 |
| -------- | -------------- | ------ | -------------------- |
| 专利上传 | PDF文件上传    | P0     | 支持单文件上传       |
| 文件存储 | MinIO对象存储  | P0     | 存储原始PDF文件      |
| 专利解析 | PDF文本提取    | P0     | Spring AI PDF Reader |
| 实体提取 | LLM实体识别    | P0     | 产品、方法、材料等   |
| 领域分析 | LLM领域分类    | P0     | IPC分类层次          |
| 向量存储 | Qdrant向量库   | P0     | 存储专利向量及元数据 |
| LLM匹配  | 语义相似度匹配 | P0     | 基于向量的技术匹配   |
| ES检索   | 全文检索       | P1     | 关键词检索           |
| 用户管理 | 登录注册       | P1     | Sa-Token认证         |

### 1.3 MVP不包含的功能（后续迭代）

- mangoDB存储聊天历史
- 可视化分析图表
- 批量导入CSV
- 专利监控预警
- 角色权限管理
- 知识图谱构建

---

## 2. 系统架构设计

### 2.1 MVP系统架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         前端层 (Vue 3 + Element Plus)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  专利上传    │  │  专利列表    │  │  技术匹配    │  │  检索界面    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                              Nginx反向代理
                                    │
┌─────────────────────────────────────────────────────────────────────────┐
│                    后端服务层 (Spring Boot 3.2 + Spring AI 1.0.3)        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ PatentService│  │ LLMService   │  │ SearchService│  │ MatchService │ │
│  │  专利管理    │  │  LLM调用     │  │  检索服务    │  │  匹配服务    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘ │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │ FileService  │  │ EntityService│  │ VectorService│                   │
│  │  MinIO文件   │  │  实体提取    │  │  向量服务    │                   │
│  └──────────────┘  └──────────────┘  └──────────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│      MySQL       │   │   Elasticsearch  │   │      Qdrant      │
│    关系数据      │   │     全文检索     │   │    向量检索      │
└──────────────────┘   └──────────────────┘   └──────────────────┘
            │                                           │
            ▼                                           ▼
┌──────────────────┐                       ┌──────────────────┐
│      MinIO       │                       │  LLM Service     │
│    文件存储      │                       │ (通义千问/Ollama) │
└──────────────────┘                       └──────────────────┘
```

### 2.2 核心数据处理流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           专利处理流程                                   │
└─────────────────────────────────────────────────────────────────────────┘

用户上传PDF ──▶ MinIO存储 ──▶ Spring AI PDF解析 ──▶ MySQL保存基础信息
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │     LLM实体+领域提取          │
                    │   (通义千问/Ollama Qwen2.5)   │
                    └───────────────────────────────┘
                           │              │
                           ▼              ▼
                    ┌───────────┐  ┌───────────┐
                    │  实体列表  │  │  IPC分类  │
                    │  关键词   │  │  技术领域  │
                    └───────────┘  └───────────┘
                           │              │
                           └──────┬───────┘
                                  ▼
                    ┌───────────────────────────────┐
                    │      向量化 (Embedding)       │
                    │    通义千问 text-embedding-v3  │
                    └───────────────────────────────┘
                           │              │
                           ▼              ▼
                    ┌───────────┐  ┌───────────┐
                    │  Qdrant   │  │    ES     │
                    │ 向量+元数据│  │ 全文索引  │
                    └───────────┘  └───────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                           技术匹配流程                                   │
└─────────────────────────────────────────────────────────────────────────┘

查询文本 ──▶ LLM提取实体/领域 ──▶ ┌─────────────────┐
                                  │  混合检索        │
                                  │ Qdrant向量检索   │
                                  │ ES关键词检索     │
                                  └─────────────────┘
                                          │
                                          ▼
                                  ┌─────────────────┐
                                  │   LLM精排       │
                                  │ 实体/领域匹配度  │
                                  └─────────────────┘
                                          │
                                          ▼
                                  ┌─────────────────┐
                                  │   返回结果      │
                                  │ 匹配专利+解释   │
                                  └─────────────────┘
```

---

## 3. 技术选型与版本

### 3.1 后端技术栈

| 技术                   | 版本   | 选型理由                     |
| ---------------------- | ------ | ---------------------------- |
| **Java**         | 17 LTS | Spring Boot 3最低要求        |
| **Spring Boot**  | 3.2.5  | 与Spring AI 1.0.3兼容        |
| **Spring AI**    | 1.0.3  | 原生支持Qdrant/OpenAI/Ollama |
| **MyBatis-Plus** | 3.5.7  | 增强版MyBatis                |
| **Sa-Token**     | 1.39.0 | 轻量级权限认证               |
| **MinIO SDK**    | 8.5.9  | 对象存储操作                 |
| **Knife4j**      | 4.5.0  | API文档                      |
| **Hutool**       | 5.8.26 | 工具集                       |

### 3.2 数据存储层

| 技术                    | 版本   | 用途                 |
| ----------------------- | ------ | -------------------- |
| **MySQL**         | 8.0+   | 专利元数据、用户数据 |
| **Redis**         | 7.x    | 缓存、会话           |
| **Elasticsearch** | 8.12+  | 全文检索             |
| **IK分词器**      | 8.12.0 | 中文分词             |
| **Qdrant**        | 1.8+   | 向量存储             |
| **MinIO**         | Latest | PDF文件存储          |

### 3.3 AI/LLM层

| 组件                    | 在线模式                            | 离线模式            |
| ----------------------- | ----------------------------------- | ------------------- |
| **Chat模型**      | 通义千问 qwen-plus                  | Ollama + Qwen2.5:7b |
| **Embedding模型** | 通义千问 text-embedding-v3 (1536维) | nomic-embed-text    |

### 3.4 前端技术栈

| 技术                   | 版本 | 用途       |
| ---------------------- | ---- | ---------- |
| **Vue.js**       | 3.4+ | 前端框架   |
| **Vite**         | 5.x  | 构建工具   |
| **Element Plus** | 2.7+ | UI组件库   |
| **Pinia**        | 2.x  | 状态管理   |
| **Axios**        | 1.6+ | HTTP请求   |
| **ECharts**      | 5.5+ | 图表可视化 |

---

## 4. 数据库设计

### 4.1 ER关系图

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│    sys_user     │       │     patent      │       │  patent_entity  │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │◄──┬───│ patent_id (FK)  │
│ username        │       │ publication_no  │   │   │ entity_name     │
│ password        │       │ title           │   │   │ entity_type     │
│ nickname        │───┐   │ applicant       │   │   └─────────────────┘
│ role            │   │   │ abstract        │   │
│ status          │   │   │ file_path       │   │   ┌─────────────────┐
│ created_at      │   │   │ source_type     │   │   │  patent_domain  │
│ updated_at      │   │   │ parse_status    │   │   ├─────────────────┤
└─────────────────┘   │   │ created_by (FK) │◄──┼───│ patent_id (FK)  │
        │             │   │ created_at      │   │   │ domain_code     │
        │             │   │ updated_at      │   │   │ domain_level    │
        │             │   └─────────────────┘   │   └─────────────────┘
        │             │           │             │
        │             └───────────┘             │   ┌─────────────────┐
        │                                       │   │  patent_vector  │
        │                                       │   ├─────────────────┤
        │                                       ├───│ patent_id (FK)  │
        │                                       │   │ vector_id       │
        │                                       │   │ embedding_model │
        │                                       │   └─────────────────┘
        │                                       │
        │         ┌─────────────────────────────┘
        │         │
        │         │   ┌─────────────────────────┐
        │         │   │      match_record       │
        │         │   ├─────────────────────────┤
        └─────────┼───│ user_id (FK)            │
                  │   │ match_mode              │  ← PATENT/TEXT
                  ├───│ source_patent_id (FK)   │  ← 专利匹配时使用
                  │   │ query_text              │  ← 文本查询时使用
                  │   │ query_entities          │
                  │   │ query_domain            │
                  └───│ target_patent_id (FK)   │
                      │ similarity_score        │
                      │ match_type              │
                      │ match_reason            │
                      └─────────────────────────┘
```

### 4.2 MySQL核心表

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(32) NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 2. 专利基础信息表
-- ============================================================
CREATE TABLE patent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '专利ID',
    publication_no VARCHAR(100) COMMENT '公开号/专利号',
    title VARCHAR(500) NOT NULL COMMENT '专利名称',
    applicant VARCHAR(255) COMMENT '申请人',
    abstract TEXT COMMENT '专利摘要',
    apply_date DATE COMMENT '申请日期',
    file_path VARCHAR(255) COMMENT 'MinIO文件路径',
    source_type VARCHAR(32) DEFAULT 'FILE' COMMENT '来源类型：FILE-PDF上传/TEXT-文本录入',
    parse_status VARCHAR(32) DEFAULT 'PENDING' COMMENT '解析状态：PENDING/PARSING/EXTRACTING/VECTORIZING/SUCCESS/FAILED',
    created_by BIGINT COMMENT '创建用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_publication_no (publication_no),
    INDEX idx_parse_status (parse_status),
    INDEX idx_created_by (created_by),
    INDEX idx_applicant (applicant),
    CONSTRAINT fk_patent_user FOREIGN KEY (created_by) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利基础信息表';

-- ============================================================
-- 3. 专利实体表（LLM提取的技术实体）
-- ============================================================
CREATE TABLE patent_entity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '实体ID',
    patent_id BIGINT NOT NULL COMMENT '专利ID',
    entity_name VARCHAR(255) NOT NULL COMMENT '实体名称',
    entity_type VARCHAR(64) NOT NULL COMMENT '实体类型：PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION',
    importance VARCHAR(20) DEFAULT 'medium' COMMENT '重要性：high/medium/low',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_patent_id (patent_id),
    INDEX idx_entity_name (entity_name),
    INDEX idx_entity_type (entity_type),
    CONSTRAINT fk_entity_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利实体表';

-- ============================================================
-- 4. 专利技术领域表（IPC/CPC分类，支持层次化）
-- ============================================================
CREATE TABLE patent_domain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '领域ID',
    patent_id BIGINT NOT NULL COMMENT '专利ID',
    domain_code VARCHAR(32) NOT NULL COMMENT '领域代码（如G06F16/30）',
    domain_level INT NOT NULL COMMENT '领域层级：1-部/2-大类/3-小类/4-主组/5-分组',
    domain_desc VARCHAR(200) COMMENT '领域描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_patent_id (patent_id),
    INDEX idx_domain_code (domain_code),
    INDEX idx_domain_level (domain_level),
    CONSTRAINT fk_domain_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利技术领域表';

-- ============================================================
-- 5. 专利向量映射表（与Qdrant的关联）
-- ============================================================
CREATE TABLE patent_vector (
    patent_id BIGINT PRIMARY KEY COMMENT '专利ID',
    vector_id VARCHAR(128) NOT NULL COMMENT 'Qdrant向量ID（UUID）',
    embedding_model VARCHAR(64) NOT NULL COMMENT '向量模型：text-embedding-v3/nomic-embed-text',
    vector_dim INT COMMENT '向量维度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_vector_patent FOREIGN KEY (patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专利向量映射表';

-- ============================================================
-- 6. 匹配记录表（统一保存专利匹配和文本查询）
-- ============================================================
CREATE TABLE match_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '匹配记录ID',
    user_id BIGINT COMMENT '用户ID',

    -- 查询来源（二选一）
    match_mode VARCHAR(16) NOT NULL COMMENT '匹配模式：PATENT-专利匹配/TEXT-文本查询',
    source_patent_id BIGINT COMMENT '源专利ID（专利匹配时使用）',
    query_text TEXT COMMENT '查询文本（文本查询时使用）',

    -- 查询分析结果
    query_entities TEXT COMMENT '查询提取的实体JSON',
    query_domain VARCHAR(32) COMMENT '查询的技术领域',

    -- 匹配目标
    target_patent_id BIGINT NOT NULL COMMENT '目标专利ID',

    -- 匹配结果
    similarity_score DECIMAL(5,4) COMMENT '相似度评分（0.0000-1.0000）',
    match_type VARCHAR(32) COMMENT '匹配类型：VECTOR/KEYWORD/HYBRID',
    entity_match_count INT COMMENT '实体匹配数量',
    domain_match TINYINT COMMENT '领域是否匹配：1-是/0-否',
    match_reason TEXT COMMENT '匹配原因（LLM生成）',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_match_mode (match_mode),
    INDEX idx_source_patent (source_patent_id),
    INDEX idx_target_patent (target_patent_id),
    INDEX idx_similarity (similarity_score),
    INDEX idx_created_at (created_at),

    -- 外键
    CONSTRAINT fk_match_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_match_source FOREIGN KEY (source_patent_id) REFERENCES patent(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_target FOREIGN KEY (target_patent_id) REFERENCES patent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匹配记录表';

### 4.3 表设计说明

| 表名              | 用途         | 关键设计                                                                |
| ----------------- | ------------ | ----------------------------------------------------------------------- |
| `sys_user`      | 用户管理     | 支持admin/user角色                                                      |
| `patent`        | 专利基础信息 | source_type区分FILE/TEXT来源；parse_status追踪处理状态                  |
| `patent_entity` | 技术实体     | LLM提取的实体，支持6种类型 + 重要性标记                                 |
| `patent_domain` | 技术领域     | **支持层次化IPC分类**（5个层级）                                  |
| `patent_vector` | 向量映射     | **独立表解耦**，记录Qdrant向量ID和模型信息                        |
| `match_record`  | 匹配记录     | **统一管理专利匹配和文本查询**，match_mode区分PATENT/TEXT两种模式 |

### 4.4 match_record 使用场景

| 场景            | match_mode | source_patent_id | query_text   | target_patent_id |
| --------------- | ---------- | ---------------- | ------------ | ---------------- |
| 专利A找相似专利 | `PATENT` | 专利A的ID        | NULL         | 匹配到的专利ID   |
| 文本查询找专利  | `TEXT`   | NULL             | 查询文本内容 | 匹配到的专利ID   |

### 4.5 解析状态流转

```
PENDING → PARSING → EXTRACTING → VECTORIZING → SUCCESS
    │         │          │            │
    └─────────┴──────────┴────────────┴──────→ FAILED
```

| 状态            | 说明                        |
| --------------- | --------------------------- |
| `PENDING`     | 待处理（刚上传）            |
| `PARSING`     | PDF解析中                   |
| `EXTRACTING`  | LLM实体/领域提取中          |
| `VECTORIZING` | 向量化存储中                |
| `SUCCESS`     | 处理完成                    |
| `FAILED`      | 处理失败（查看parse_error） |

### 4.6 领域层级说明（domain_level）

| 层级 | 说明             | 示例                |
| ---- | ---------------- | ------------------- |
| 1    | 部(Section)      | G-物理              |
| 2    | 大类(Class)      | G06-计算；计数      |
| 3    | 小类(Subclass)   | G06F-电数字数据处理 |
| 4    | 主组(Main Group) | G06F16-信息检索     |
| 5    | 分组(Group)      | G06F16/30-代码转换  |

### 4.7 Elasticsearch索引设计

```json
{
  "patent_index": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "analysis": {
        "analyzer": {
          "ik_max_word": { "type": "custom", "tokenizer": "ik_max_word" },
          "ik_smart": { "type": "custom", "tokenizer": "ik_smart" }
        }
      }
    },
    "mappings": {
      "properties": {
        "id": { "type": "keyword" },
        "publication_no": { "type": "keyword" },
        "title": { 
          "type": "text", 
          "analyzer": "ik_max_word", 
          "search_analyzer": "ik_smart",
          "fields": { "keyword": { "type": "keyword" } }
        },
        "abstract": { 
          "type": "text", 
          "analyzer": "ik_max_word", 
          "search_analyzer": "ik_smart" 
        },
        "claims": { 
          "type": "text", 
          "analyzer": "ik_max_word" 
        },
        "applicant": { "type": "keyword" },
        "apply_date": { "type": "date", "format": "yyyy-MM-dd" },
        "domain_codes": { "type": "keyword" },
        "domain_section": { "type": "keyword" },
        "entities": { 
          "type": "text", 
          "analyzer": "ik_max_word",
          "fields": { "keyword": { "type": "keyword" } }
        },
        "entity_types": { "type": "keyword" },
        "parse_status": { "type": "keyword" },
        "created_at": { "type": "date" }
      }
    }
  }
}
```

### 4.8 Qdrant Collection设计

```yaml
collection_name: patent_vectors
vectors:
  size: 1536  # 通义千问text-embedding-v3维度
  distance: Cosine

# HNSW索引配置（优化检索性能）
hnsw_config:
  m: 16
  ef_construct: 100

# Payload元数据Schema
payload_schema:
  patent_id: integer          # 专利ID（关联MySQL）
  publication_no: keyword     # 公开号
  title: keyword              # 标题
  applicant: keyword          # 申请人
  domain_section: keyword     # IPC部（G/H/A等，用于过滤）
  domain_codes: keyword[]     # 完整IPC编码列表
  entities: keyword[]         # 实体名称列表
  entity_types: keyword[]     # 实体类型列表
  embedding_model: keyword    # 向量模型名称

# 创建Payload索引（优化过滤性能）
indexes:
  - field_name: domain_section
    field_schema: keyword
  - field_name: applicant
    field_schema: keyword
```

---

## 5. 核心功能实现步骤

### 功能1：专利PDF上传（MinIO存储）

**流程**：`用户上传PDF → 后端接收 → 上传MinIO → 保存MySQL → 返回专利ID`

**实现要点**：

```java
// 1. MinIO配置
@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}

// 2. 上传服务核心逻辑
public Long uploadPatentPdf(MultipartFile file, String publicationNo, Long userId) {
    // 生成唯一文件名
    String objectName = "patents/" + publicationNo + "/" + UUID.randomUUID() + ".pdf";
  
    // 上传MinIO
    minioClient.putObject(PutObjectArgs.builder()
        .bucket(bucketName)
        .object(objectName)
        .stream(file.getInputStream(), file.getSize(), -1)
        .contentType("application/pdf")
        .build());
  
    // 保存MySQL记录（使用新表结构）
    Patent patent = new Patent();
    patent.setPublicationNo(publicationNo);
    patent.setFilePath(objectName);
    patent.setSourceType("FILE");           // 来源类型：文件上传
    patent.setParseStatus("PENDING");       // 初始状态：待处理
    patent.setCreatedBy(userId);
    patentMapper.insert(patent);
  
    return patent.getId();
}
```

**接口**：

```
POST /api/patent/upload
Content-Type: multipart/form-data
参数: file(PDF文件), publicationNo(公开号)
返回: { code, data: { patentId, filePath, parseStatus } }
```

---

### 功能2：专利PDF解析

**流程**：`从MinIO读取PDF → Spring AI PDF Reader解析 → 提取文本内容 → 分段处理`

**实现要点**：

```java
// 使用Spring AI PDF Reader解析
public PatentContent parsePdf(String pdfPath) {
    // 1. 从MinIO获取PDF流
    InputStream pdfStream = minioClient.getObject(GetObjectArgs.builder()
        .bucket(bucketName)
        .object(pdfPath)
        .build());
  
    // 2. 使用Spring AI PagePdfDocumentReader解析
    Resource pdfResource = new InputStreamResource(pdfStream);
    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource,
        PdfDocumentReaderConfig.builder()
            .withPagesPerDocument(1)
            .build());
  
    // 3. 提取文档内容
    List<Document> documents = pdfReader.read();
  
    // 4. 合并文本内容
    String fullText = documents.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n"));
  
    // 5. 解析专利结构（标题、摘要、权利要求）
    return parsePatentStructure(fullText);
}
```

**Maven依赖**：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pdf-document-reader</artifactId>
</dependency>
```

---

### 功能3：LLM实体和领域提取

**流程**：`专利文本 → 构造Prompt → 调用LLM → 解析JSON结果 → 保存实体/领域`

**实体提取Prompt模板**：

```java
private static final String ENTITY_EXTRACTION_PROMPT = """
你是专利技术分析专家，请从以下专利文本中提取实体和领域信息。

专利文本：
{patentText}

请按JSON格式输出：
{
  "entities": [
    {"text": "实体名称", "type": "PRODUCT|METHOD|MATERIAL|COMPONENT|EFFECT|APPLICATION", "importance": "high|medium|low"}
  ],
  "domain": {
    "section": "IPC部(如G)",
    "mainClass": "IPC大类(如G06)",
    "subclass": "IPC小类(如G06F)",
    "fullCode": "完整IPC编码",
    "description": "领域描述"
  },
  "keywords": ["关键词1", "关键词2"]
}

只输出JSON，不要其他解释。
""";
```

**实现要点（Spring AI ChatClient）**：

```java
// 定义返回结构
record PatentAnalysis(List<Entity> entities, Domain domain, List<String> keywords) {}
record Entity(String text, String type, String importance) {}
record Domain(String section, String mainClass, String subclass, String fullCode, String description) {}

// 调用LLM获取结构化输出
public PatentAnalysis extractEntitiesAndDomain(String patentText) {
    return chatClient.prompt()
        .user(u -> u.text(ENTITY_EXTRACTION_PROMPT).param("patentText", patentText))
        .call()
        .entity(PatentAnalysis.class);
}

// 保存到数据库（使用新表结构）
public void saveAnalysisResult(Long patentId, PatentAnalysis result) {
    // 保存实体到 patent_entity 表
    result.entities().forEach(entity -> {
        PatentEntity pe = new PatentEntity();
        pe.setPatentId(patentId);
        pe.setEntityName(entity.text());      // 使用 entity_name
        pe.setEntityType(entity.type());
        pe.setImportance(entity.importance());
        patentEntityMapper.insert(pe);
    });
  
    // 保存层次化领域到 patent_domain 表（每个层级一条记录）
    Domain domain = result.domain();
    saveDomainLevel(patentId, domain.section(), 1, "部");
    saveDomainLevel(patentId, domain.section() + domain.mainClass(), 2, "大类");
    saveDomainLevel(patentId, domain.section() + domain.mainClass() + domain.subclass(), 3, "小类");
    saveDomainLevel(patentId, domain.fullCode(), 5, domain.description());
  
    // 更新专利解析状态
    patentMapper.updateParseStatus(patentId, "EXTRACTING", null);
}

// 保存单个领域层级
private void saveDomainLevel(Long patentId, String code, int level, String desc) {
    PatentDomain pd = new PatentDomain();
    pd.setPatentId(patentId);
    pd.setDomainCode(code);
    pd.setDomainLevel(level);
    pd.setDomainDesc(desc);
    patentDomainMapper.insert(pd);
}
```

**双模式LLM配置**：

```yaml
spring:
  ai:
    # 在线模式 - 通义千问
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
    # 离线模式 - Ollama
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:7b
```

---

### 功能4：专利向量化存储（Qdrant）

**流程**：`专利文本+实体 → Embedding生成 → 存储Qdrant → 更新MySQL状态`

**实现要点**：

```java
// Qdrant配置
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: patent_vectors
        initialize-schema: true
    dashscope:
      embedding:
        options:
          model: text-embedding-v3

// 向量化存储服务（使用新表结构）
public void storePatentVector(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains) {
    // 1. 构造增强文本（标题 + 摘要 + 实体关键词）
    List<String> entityNames = entities.stream()
        .map(PatentEntity::getEntityName)
        .toList();
    List<String> entityTypes = entities.stream()
        .map(PatentEntity::getEntityType)
        .distinct()
        .toList();
  
    String enhancedText = String.format(
        "标题：%s\n摘要：%s\n关键技术：%s",
        patent.getTitle(),
        patent.getAbstract(),
        String.join("、", entityNames)
    );
  
    // 2. 获取领域信息
    String domainSection = domains.stream()
        .filter(d -> d.getDomainLevel() == 1)
        .map(PatentDomain::getDomainCode)
        .findFirst().orElse("");
    List<String> domainCodes = domains.stream()
        .map(PatentDomain::getDomainCode)
        .toList();
  
    // 3. 创建Document对象（含元数据用于Qdrant过滤）
    Document document = new Document(enhancedText, Map.of(
        "patent_id", patent.getId(),
        "publication_no", patent.getPublicationNo(),
        "title", patent.getTitle(),
        "applicant", patent.getApplicant() != null ? patent.getApplicant() : "",
        "domain_section", domainSection,
        "domain_codes", domainCodes,
        "entities", entityNames,
        "entity_types", entityTypes,
        "embedding_model", embeddingModelName
    ));
  
    // 4. 存储到Qdrant（Spring AI自动生成Embedding）
    vectorStore.add(List.of(document));
  
    // 5. 保存向量映射到 patent_vector 表
    PatentVector pv = new PatentVector();
    pv.setPatentId(patent.getId());
    pv.setVectorId(document.getId());
    pv.setEmbeddingModel(embeddingModelName);
    pv.setVectorDim(1536);
    patentVectorMapper.insert(pv);
  
    // 6. 更新专利状态为成功
    patentMapper.updateParseStatus(patent.getId(), "SUCCESS", null);
}
```

---

### 功能5：LLM技术匹配

**流程**：`查询文本 → 实体提取 → 向量检索(Qdrant) → LLM精排 → 返回匹配结果`

**实现要点**：

```java
// 1. 向量相似度检索（支持IPC过滤）
public List<Document> semanticSearch(String queryText, String ipcFilter, int topK) {
    SearchRequest.Builder builder = SearchRequest.builder()
        .query(queryText)
        .topK(topK)
        .similarityThreshold(0.6);
  
    // 可选：领域过滤
    if (ipcFilter != null) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        builder.filterExpression(b.eq("ipc_section", ipcFilter).build());
    }
  
    return vectorStore.similaritySearch(builder.build());
}

// 2. LLM精排（评估匹配度）
private static final String RERANK_PROMPT = """
请评估以下专利与查询需求的技术匹配度，返回0-100的分数和匹配原因。

查询需求：{query}
查询实体：{queryEntities}

候选专利：
标题：{candidateTitle}
摘要：{candidateAbstract}
IPC分类：{ipcCode}

请返回JSON：{"score": 85, "reason": "匹配原因说明", "matchedEntities": ["匹配实体1"]}
""";

public MatchScore rerankWithLlm(String query, List<String> queryEntities, Patent candidate) {
    return chatClient.prompt()
        .user(u -> u.text(RERANK_PROMPT)
            .param("query", query)
            .param("queryEntities", String.join("、", queryEntities))
            .param("candidateTitle", candidate.getTitle())
            .param("candidateAbstract", candidate.getAbstract())
            .param("ipcCode", candidate.getIpcCode()))
        .call()
        .entity(MatchScore.class);
}

// 3. 完整匹配流程（使用新表结构）
public List<MatchResult> matchPatents(String queryText, String domainFilter, int topK, Long userId) {
    // Step 1: 提取查询实体
    PatentAnalysis queryAnalysis = extractEntitiesAndDomain(queryText);
  
    // Step 2: 向量检索
    List<Document> candidates = semanticSearch(queryText, domainFilter, topK * 2);
  
    // Step 3: LLM精排
    List<MatchResult> results = candidates.stream()
        .map(doc -> {
            Long patentId = (Long) doc.getMetadata().get("patent_id");
            Patent patent = patentMapper.selectById(patentId);
            MatchScore score = rerankWithLlm(queryText, queryAnalysis.keywords(), patent);
            return new MatchResult(patent, score);
        })
        .sorted(Comparator.comparing(r -> r.score().score()).reversed())
        .limit(topK)
        .toList();
  
    // Step 4: 保存匹配记录到 match_record 表（统一表）
    results.forEach(result -> {
        MatchRecord record = new MatchRecord();
        record.setUserId(userId);
        record.setMatchMode("TEXT");  // 文本查询模式
        record.setSourcePatentId(null);  // 文本查询时为null
        record.setQueryText(queryText);
        record.setQueryEntities(JSON.toJSONString(queryAnalysis.entities()));
        record.setQueryDomain(domainFilter);
        record.setTargetPatentId(result.patent().getId());
        record.setSimilarityScore(BigDecimal.valueOf(result.score().score() / 100.0));
        record.setMatchType("HYBRID");
        record.setEntityMatchCount(result.score().matchedEntities().size());
        record.setDomainMatch(result.score().domainMatched() ? 1 : 0);
        record.setMatchReason(result.score().reason());
        matchRecordMapper.insert(record);
    });
  
    return results;
}
```

**匹配结果结构**：

```java
record MatchResult(
    Long patentId,
    String patentNo,
    String title,
    String abstractText,
    String ipcCode,
    Integer score,           // LLM评分(0-100)
    String matchReason,      // 匹配原因
    List<String> matchedEntities  // 匹配的实体
) {}
```

---

### 功能6：ES关键词检索

**流程**：`关键词输入 → ES多字段检索 → IK分词匹配 → 返回结果`

**实现要点**：

```java
// ES Document定义
@Document(indexName = "patent_index")
public class PatentDocument {
    @Id
    private String id;
  
    @Field(type = FieldType.Keyword)
    private String patentNo;
  
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
  
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String abstractText;
  
    @Field(type = FieldType.Keyword)
    private String ipcCode;
  
    @Field(type = FieldType.Keyword)
    private List<String> keywords;
  
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private List<String> entities;
}

// 多字段加权检索
public List<PatentDocument> fullTextSearch(String keyword, int page, int size) {
    NativeQuery query = NativeQuery.builder()
        .withQuery(q -> q.multiMatch(mm -> mm
            .query(keyword)
            .fields(List.of(
                "title^3",        // 标题权重最高
                "abstractText^2", // 摘要权重次之
                "entities^1.5",   // 实体
                "keywords^2"      // 关键词
            ))
        ))
        .withPageable(PageRequest.of(page, size))
        .build();
  
    return elasticsearchOperations.search(query, PatentDocument.class)
        .stream()
        .map(SearchHit::getContent)
        .toList();
}
```

---

## 6. API接口设计

### 6.1 专利管理接口

| 接口                          | 方法   | 功能             | 参数                    |
| ----------------------------- | ------ | ---------------- | ----------------------- |
| `/api/patent/upload`        | POST   | 上传专利PDF      | file, publicationNo     |
| `/api/patent/text`          | POST   | 文本录入专利     | title, abstract, claims |
| `/api/patent/process/{id}`  | POST   | 触发处理流程     | -                       |
| `/api/patent/{id}`          | GET    | 获取专利详情     | -                       |
| `/api/patent/{id}/entities` | GET    | 获取专利实体列表 | -                       |
| `/api/patent/{id}/domains`  | GET    | 获取专利领域层次 | -                       |
| `/api/patent/{id}/vector`   | GET    | 获取向量信息     | -                       |
| `/api/patent/list`          | GET    | 专利列表         | page, size, parseStatus |
| `/api/patent/{id}`          | DELETE | 删除专利         | -                       |

### 6.2 检索匹配接口

| 接口                       | 方法 | 功能         | 参数                                               |
| -------------------------- | ---- | ------------ | -------------------------------------------------- |
| `/api/match`             | POST | LLM技术匹配  | query, domainFilter, topK                          |
| `/api/match/patent/{id}` | POST | 相似专利匹配 | topK                                               |
| `/api/match/history`     | GET  | 匹配历史记录 | page, size, matchMode(可选：PATENT/TEXT)           |
| `/api/search`            | GET  | ES关键词检索 | keyword, page, size                                |
| `/api/search/advanced`   | POST | 高级检索     | title, abstract, domainCode, applicant, entityType |

### 6.3 用户接口

| 接口                   | 方法 | 功能             |
| ---------------------- | ---- | ---------------- |
| `/api/auth/register` | POST | 用户注册         |
| `/api/auth/login`    | POST | 用户登录         |
| `/api/auth/logout`   | POST | 退出登录         |
| `/api/auth/info`     | GET  | 获取当前用户信息 |

### 6.4 接口响应示例

**技术匹配接口**：

```json
// POST /api/match
// Request
{
  "query": "基于深度学习的医疗图像诊断方法，自动识别CT图像中的病变区域",
  "domainFilter": "G06",
  "topK": 10
}

// Response
{
  "code": 200,
  "message": "success",
  "data": {
    "query": "...",
    "queryEntities": [
      {"name": "深度学习", "type": "METHOD"},
      {"name": "CT图像", "type": "PRODUCT"},
      {"name": "病变检测", "type": "APPLICATION"}
    ],
    "matches": [
      {
        "patentId": 1,
        "publicationNo": "CN123456789A",
        "title": "一种基于深度学习的CT图像病变检测方法",
        "abstract": "...",
        "applicant": "某医疗科技公司",
        "domainCodes": ["G06T7/00", "G06N3/08"],
        "similarityScore": 0.9234,
        "matchType": "HYBRID",
        "entityMatchCount": 3,
        "domainMatch": true,
        "matchReason": "该专利同样基于深度学习进行CT图像分析，核心技术实体高度匹配"
      }
    ],
    "totalCount": 10
  }
}
```

**专利详情接口**：

```json
// GET /api/patent/1
// Response
{
  "code": 200,
  "data": {
    "id": 1,
    "publicationNo": "CN123456789A",
    "title": "一种基于深度学习的CT图像病变检测方法",
    "applicant": "某医疗科技公司",
    "abstract": "...",
    "claims": "...",
    "applyDate": "2024-01-15",
    "filePath": "patents/CN123456789A/xxx.pdf",
    "sourceType": "FILE",
    "parseStatus": "SUCCESS",
    "entities": [
      {"id": 1, "entityName": "深度学习", "entityType": "METHOD", "importance": "high"},
      {"id": 2, "entityName": "卷积神经网络", "entityType": "METHOD", "importance": "high"},
      {"id": 3, "entityName": "CT图像", "entityType": "PRODUCT", "importance": "medium"}
    ],
    "domains": [
      {"domainCode": "G", "domainLevel": 1, "domainDesc": "物理"},
      {"domainCode": "G06", "domainLevel": 2, "domainDesc": "计算；计数"},
      {"domainCode": "G06T", "domainLevel": 3, "domainDesc": "图像数据处理"},
      {"domainCode": "G06T7/00", "domainLevel": 5, "domainDesc": "图像分析"}
    ],
    "vector": {
      "vectorId": "uuid-xxx",
      "embeddingModel": "text-embedding-v3",
      "vectorDim": 1536
    },
    "createdAt": "2024-01-20 10:30:00"
  }
}
```

---

## 7. 配置文件

### 7.1 application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: patent-match-system
  
  # MySQL配置
  datasource:
    url: jdbc:mysql://localhost:3306/patent_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
    elasticsearch:
      uris: http://localhost:9200

  # Spring AI配置
  ai:
    # 通义千问配置（在线模式）
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-plus
      embedding:
        options:
          model: text-embedding-v3
  
    # Ollama配置（离线模式）
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen2.5:7b
      embedding:
        options:
          model: nomic-embed-text
  
    # Qdrant向量存储
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: patent_vectors
        initialize-schema: true

# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.patent.model.entity
  configuration:
    map-underscore-to-camel-case: true

# Sa-Token配置
sa-token:
  token-name: Authorization
  timeout: 86400
  is-concurrent: true
  is-share: false

# MinIO配置
minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-name: patent-files

# 自定义配置
patent:
  llm-mode: ${LLM_MODE:online}  # online/offline
  match-top-k: 10
  similarity-threshold: 0.6
```

### 7.2 pom.xml核心依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
    </parent>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.0.3</spring-ai.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <satoken.version>1.39.0</satoken.version>
        <minio.version>8.5.9</minio.version>
        <knife4j.version>4.5.0</knife4j.version>
        <hutool.version>5.8.26</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring AI - 通义千问 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-dashscope</artifactId>
        </dependency>
        <!-- Spring AI - Ollama -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-ollama</artifactId>
        </dependency>
        <!-- Spring AI - Qdrant -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-qdrant</artifactId>
        </dependency>
        <!-- Spring AI - PDF解析 -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-pdf-document-reader</artifactId>
        </dependency>

        <!-- 数据库 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>

        <!-- Sa-Token -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>${satoken.version}</version>
        </dependency>

        <!-- MinIO -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>

        <!-- 工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <url>https://repo.spring.io/milestone</url>
        </repository>
    </repositories>
</project>
```

---

## 8. 部署方案

### 8.1 Docker Compose

```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: patent-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: patent_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - patent-network

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: patent-redis
    ports:
      - "6379:6379"
    networks:
      - patent-network

  # Elasticsearch搜索引擎
  elasticsearch:
    image: elasticsearch:8.12.0
    container_name: patent-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    networks:
      - patent-network

  # Qdrant向量数据库
  qdrant:
    image: qdrant/qdrant:v1.8.0
    container_name: patent-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    networks:
      - patent-network

  # MinIO对象存储
  minio:
    image: minio/minio:latest
    container_name: patent-minio
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    networks:
      - patent-network

  # Ollama本地LLM（离线模式）
  ollama:
    image: ollama/ollama:latest
    container_name: patent-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - patent-network

  # 后端服务
  backend:
    build: ./backend
    container_name: patent-backend
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - LLM_MODE=${LLM_MODE:-online}
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - elasticsearch
      - qdrant
      - minio
    networks:
      - patent-network

  # 前端服务
  frontend:
    build: ./frontend
    container_name: patent-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - patent-network

volumes:
  mysql_data:
  es_data:
  qdrant_data:
  minio_data:
  ollama_data:

networks:
  patent-network:
    driver: bridge
```

### 8.2 环境变量

```bash
# .env
DASHSCOPE_API_KEY=your-dashscope-api-key
LLM_MODE=online  # online/offline
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### 8.3 启动命令

```bash
# 启动所有服务
docker-compose up -d

# 仅启动基础设施
docker-compose up -d mysql redis elasticsearch qdrant minio

# 离线模式（启动Ollama并拉取模型）
docker-compose up -d ollama
docker exec patent-ollama ollama pull qwen2.5:7b
docker exec patent-ollama ollama pull nomic-embed-text
```

---

## 9. 开发计划

### 9.1 开发里程碑

| 阶段              | 周期   | 任务                                                      |
| ----------------- | ------ | --------------------------------------------------------- |
| **Phase 1** | Week 1 | 项目骨架搭建、Docker环境、MySQL/Redis/ES/Qdrant/MinIO部署 |
| **Phase 2** | Week 2 | MinIO文件上传、PDF解析、专利CRUD接口                      |
| **Phase 3** | Week 3 | Spring AI集成、LLM实体提取、领域分类                      |
| **Phase 4** | Week 4 | Qdrant向量存储、ES索引、混合检索                          |
| **Phase 5** | Week 5 | LLM技术匹配、精排算法、前端界面                           |
| **Phase 6** | Week 6 | 功能测试、性能优化、部署上线                              |

### 9.2 MVP功能边界

| 包含（MVP）         | 不包含（后续迭代） |
| ------------------- | ------------------ |
| PDF上传/MinIO存储   | 可视化分析图表     |
| PDF解析提取文本     | 批量CSV导入        |
| LLM实体识别         | 专利监控预警       |
| LLM领域分类         | 角色权限管理       |
| Qdrant向量存储/检索 | 知识图谱构建       |
| ES全文检索          | 系统日志监控       |
| LLM技术匹配+精排    | -                  |
| Sa-Token用户认证    | -                  |

---

**文档版本**：v2.0
**编写日期**：2026年2月1日
**文档状态**：MVP实现方案定稿
