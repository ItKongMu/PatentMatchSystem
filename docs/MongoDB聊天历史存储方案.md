# MongoDB 聊天历史存储方案

## 1. 概述

### 1.1 背景

当前系统的聊天记忆使用 Spring AI 的 `MessageWindowChatMemory`，存储在内存中：

```java
// 当前实现（ChatServiceImpl.java）
this.chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(20)
        .build();
```

**现有问题：**
- 服务重启后聊天历史丢失
- 无法跨设备同步会话
- 无法查看历史会话记录
- 不支持多会话管理

### 1.2 目标

引入 MongoDB 存储聊天历史，实现：
- **用户级管理**：通过 `userId` 管理用户的所有聊天记录
- **多会话支持**：通过 `sessionId` 管理用户的多个独立会话
- **历史持久化**：会话数据持久存储，支持历史查询
- **与 Spring AI 集成**：自定义 `ChatMemoryRepository` 实现

### 1.3 可行性分析

| 评估项 | 状态 | 说明 |
|--------|------|------|
| 技术兼容性 | ✅ 可行 | Spring Boot 3.2.5 + Spring Data MongoDB 完全兼容 |
| Spring AI 集成 | ✅ 可行 | 可通过自定义 `ChatMemoryRepository` 实现 |
| 现有架构 | ✅ 兼容 | 不影响 MySQL、Redis、ES 等现有存储 |
| 用户体系 | ✅ 已具备 | `SysUser` 实体已有 `id` 字段可作为 `userId` |
| 运维成本 | ⚠️ 中等 | 需要额外部署 MongoDB 服务 |

---

## 2. 技术方案

### 2.1 数据模型设计

采用 **分集合存储** 策略，避免单文档超过 MongoDB 16MB 限制：

#### 2.1.1 会话集合（chat_sessions）

```json
{
  "_id": "ObjectId",
  "sessionId": "uuid-string",           // 会话唯一标识
  "userId": 1,                          // 关联用户ID（SysUser.id）
  "title": "关于深度学习专利的咨询",     // 会话标题（可从首条消息生成）
  "createdAt": "ISODate",               // 创建时间
  "updatedAt": "ISODate",               // 最后更新时间
  "messageCount": 10,                   // 消息数量
  "status": "active",                   // 状态：active/archived/deleted
  "metadata": {                         // 扩展元数据
    "lastTopic": "深度学习",
    "toolsUsed": ["searchPatents", "matchPatents"]
  }
}
```

#### 2.1.2 消息集合（chat_messages）

```json
{
  "_id": "ObjectId",
  "sessionId": "uuid-string",           // 关联会话ID
  "userId": 1,                          // 冗余用户ID（便于查询）
  "role": "user|assistant|system",      // 消息角色
  "content": "帮我搜索深度学习相关专利", // 消息内容
  "timestamp": "ISODate",               // 消息时间
  "sequence": 1,                        // 消息序号（用于排序）
  "metadata": {                         // 扩展元数据
    "toolCalls": [...],                 // 工具调用记录
    "patents": [...],                   // 关联专利结果
    "tokens": 150                       // Token 消耗
  }
}
```

### 2.2 索引设计

```javascript
// chat_sessions 集合索引
db.chat_sessions.createIndex({ "userId": 1, "updatedAt": -1 })  // 用户会话列表
db.chat_sessions.createIndex({ "sessionId": 1 }, { unique: true })  // 会话ID唯一

// chat_messages 集合索引
db.chat_messages.createIndex({ "sessionId": 1, "sequence": 1 })  // 会话消息排序
db.chat_messages.createIndex({ "userId": 1, "timestamp": -1 })   // 用户消息时间线
db.chat_messages.createIndex({ "sessionId": 1, "timestamp": -1 }) // 会话消息时间线
```

### 2.3 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue.js)                            │
├─────────────────────────────────────────────────────────────────┤
│  ChatController                                                  │
│  ├── /api/chat/stream (流式对话)                                │
│  ├── /api/chat/sessions (会话列表)                              │
│  └── /api/chat/sessions/{id}/messages (会话消息)                │
├─────────────────────────────────────────────────────────────────┤
│  ChatService                                                     │
│  ├── chatStreamSSE() ─── 使用 MongoChatMemoryRepository         │
│  ├── getUserSessions() ─── 查询用户会话列表                      │
│  └── getSessionMessages() ─── 查询会话消息历史                   │
├─────────────────────────────────────────────────────────────────┤
│  MongoChatMemoryRepository (自定义实现)                          │
│  ├── implements ChatMemoryRepository                             │
│  ├── 读写 chat_sessions 集合                                     │
│  └── 读写 chat_messages 集合                                     │
├─────────────────────────────────────────────────────────────────┤
│  MongoDB                              │  其他存储                │
│  ├── chat_sessions                    │  ├── MySQL (用户/专利)   │
│  └── chat_messages                    │  ├── Redis (缓存/会话)   │
│                                       │  ├── ES (全文检索)       │
│                                       │  └── Qdrant (向量)       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 实现步骤

### 3.1 添加 Maven 依赖

```xml
<!-- pom.xml -->
<!-- MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 3.2 配置 MongoDB 连接

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/patent_chat
      # 或使用详细配置
      # host: localhost
      # port: 27017
      # database: patent_chat
      # username: patent_user
      # password: your_password
      # authentication-database: admin
```

### 3.3 实体类定义

#### ChatSession.java

```java
package com.patent.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天会话实体
 */
@Data
@Document(collection = "chat_sessions")
@CompoundIndex(name = "user_updated_idx", def = "{'userId': 1, 'updatedAt': -1}")
public class ChatSession {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String sessionId;
    
    @Indexed
    private Long userId;
    
    private String title;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Integer messageCount;
    
    private String status; // active, archived, deleted
    
    private Map<String, Object> metadata;
}
```

#### ChatMessage.java

```java
package com.patent.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息实体
 */
@Data
@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "session_sequence_idx", def = "{'sessionId': 1, 'sequence': 1}"),
    @CompoundIndex(name = "session_time_idx", def = "{'sessionId': 1, 'timestamp': -1}")
})
public class ChatMessage {
    
    @Id
    private String id;
    
    private String sessionId;
    
    private Long userId;
    
    private String role; // user, assistant, system
    
    private String content;
    
    private LocalDateTime timestamp;
    
    private Integer sequence;
    
    private Map<String, Object> metadata;
}
```

### 3.4 Repository 接口

```java
package com.patent.repository;

import com.patent.model.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    Optional<ChatSession> findBySessionId(String sessionId);
    
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
    
    Page<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status, Pageable pageable);
    
    void deleteBySessionId(String sessionId);
}
```

```java
package com.patent.repository;

import com.patent.model.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    List<ChatMessage> findBySessionIdOrderBySequenceAsc(String sessionId);
    
    Page<ChatMessage> findBySessionIdOrderBySequenceDesc(String sessionId, Pageable pageable);
    
    List<ChatMessage> findTop20BySessionIdOrderBySequenceDesc(String sessionId);
    
    void deleteBySessionId(String sessionId);
    
    long countBySessionId(String sessionId);
}
```

### 3.5 自定义 ChatMemoryRepository

```java
package com.patent.config;

import com.patent.model.entity.ChatMessage;
import com.patent.model.entity.ChatSession;
import com.patent.repository.ChatMessageRepository;
import com.patent.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB 实现的 ChatMemoryRepository
 * 用于持久化聊天记忆到 MongoDB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoChatMemoryRepository implements ChatMemoryRepository {
    
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    
    // 用于临时存储当前用户ID（通过 ThreadLocal 传递）
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    
    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }
    
    public static void clearCurrentUserId() {
        currentUserId.remove();
    }
    
    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderBySequenceAsc(conversationId);
        return messages.stream()
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Long userId = currentUserId.get();
        
        // 确保会话存在
        ChatSession session = sessionRepository.findBySessionId(conversationId)
                .orElseGet(() -> createNewSession(conversationId, userId));
        
        // 获取当前最大序号
        long currentCount = messageRepository.countBySessionId(conversationId);
        int sequence = (int) currentCount;
        
        // 保存新消息
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Message message : messages) {
            // 检查是否已存在（避免重复保存）
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(conversationId);
            chatMessage.setUserId(userId);
            chatMessage.setRole(message.getMessageType().getValue());
            chatMessage.setContent(message.getText());
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSequence(++sequence);
            chatMessage.setMetadata(new HashMap<>(message.getMetadata()));
            chatMessages.add(chatMessage);
        }
        
        if (!chatMessages.isEmpty()) {
            messageRepository.saveAll(chatMessages);
            
            // 更新会话信息
            session.setUpdatedAt(LocalDateTime.now());
            session.setMessageCount(sequence);
            
            // 从第一条用户消息生成标题
            if (session.getTitle() == null || session.getTitle().isEmpty()) {
                messages.stream()
                        .filter(m -> m.getMessageType() == MessageType.USER)
                        .findFirst()
                        .ifPresent(m -> {
                            String title = m.getText();
                            if (title.length() > 50) {
                                title = title.substring(0, 50) + "...";
                            }
                            session.setTitle(title);
                        });
            }
            sessionRepository.save(session);
        }
    }
    
    @Override
    public void deleteByConversationId(String conversationId) {
        messageRepository.deleteBySessionId(conversationId);
        sessionRepository.deleteBySessionId(conversationId);
        log.info("已删除会话: {}", conversationId);
    }
    
    private ChatSession createNewSession(String sessionId, Long userId) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setMessageCount(0);
        session.setStatus("active");
        session.setMetadata(new HashMap<>());
        return sessionRepository.save(session);
    }
    
    private Message toSpringAiMessage(ChatMessage chatMessage) {
        String role = chatMessage.getRole();
        String content = chatMessage.getContent();
        
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> new UserMessage(content);
        };
    }
}
```

### 3.6 修改 ChatServiceImpl

```java
// ChatServiceImpl.java 修改构造函数

public ChatServiceImpl(@Qualifier("primaryChatModel") ChatModel chatModel,
                       SearchService searchService,
                       MatchService matchService,
                       MongoChatMemoryRepository mongoChatMemoryRepository) {
    this.searchService = searchService;
    this.matchService = matchService;
    
    // 使用 MongoDB 实现的 ChatMemoryRepository
    this.chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(mongoChatMemoryRepository)
            .maxMessages(20)
            .build();
    
    // 创建 ChatClient
    this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(this)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .build();
    
    log.info("对话式检索服务初始化完成（MongoDB 持久化）");
}

// 在 chat 方法中设置当前用户ID
@Override
public ChatResponseVO chat(ChatMessageDTO dto) {
    // 获取当前登录用户ID
    Long userId = StpUtil.getLoginIdAsLong();
    MongoChatMemoryRepository.setCurrentUserId(userId);
    
    try {
        // ... 原有逻辑
    } finally {
        MongoChatMemoryRepository.clearCurrentUserId();
    }
}
```

### 3.7 新增 API 接口

```java
// ChatController.java 添加会话管理接口

/**
 * 获取用户会话列表
 */
@GetMapping("/sessions")
@Operation(summary = "获取会话列表", description = "获取当前用户的所有聊天会话")
public Result<List<ChatSessionVO>> getSessions(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size) {
    Long userId = StpUtil.getLoginIdAsLong();
    List<ChatSessionVO> sessions = chatService.getUserSessions(userId, page, size);
    return Result.success(sessions);
}

/**
 * 获取会话消息历史
 */
@GetMapping("/sessions/{sessionId}/messages")
@Operation(summary = "获取会话消息", description = "获取指定会话的消息历史")
public Result<List<ChatMessageVO>> getSessionMessages(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "50") Integer size) {
    List<ChatMessageVO> messages = chatService.getSessionMessages(sessionId, page, size);
    return Result.success(messages);
}

/**
 * 删除会话
 */
@DeleteMapping("/sessions/{sessionId}")
@Operation(summary = "删除会话", description = "删除指定的聊天会话及其所有消息")
public Result<Void> deleteSession(@PathVariable String sessionId) {
    chatService.deleteSession(sessionId);
    return Result.success(null);
}
```

---

## 4. 数据流程

### 4.1 新建会话流程

```
1. 用户发送首条消息
2. ChatService 获取当前用户 userId
3. MongoChatMemoryRepository.saveAll() 被调用
4. 检查会话不存在，创建新 ChatSession
5. 保存消息到 ChatMessage
6. 从首条消息生成会话标题
```

### 4.2 继续会话流程

```
1. 用户携带 sessionId 发送消息
2. MongoChatMemoryRepository.findByConversationId() 加载历史
3. Spring AI 将历史消息加入上下文
4. AI 生成回复
5. MongoChatMemoryRepository.saveAll() 保存新消息
6. 更新 ChatSession.updatedAt 和 messageCount
```

### 4.3 查看历史会话流程

```
1. 用户请求会话列表 GET /api/chat/sessions
2. 查询 chat_sessions (userId=当前用户, status=active)
3. 返回会话列表（按 updatedAt 降序）
4. 用户点击某会话，请求消息 GET /api/chat/sessions/{id}/messages
5. 查询 chat_messages (sessionId=选中会话)
6. 返回消息列表（按 sequence 排序）
```

---

## 5. 前端适配

### 5.1 会话列表组件

```vue
<!-- 会话侧边栏示例 -->
<template>
  <div class="session-list">
    <div class="session-header">
      <span>历史会话</span>
      <el-button size="small" @click="createNewSession">新建会话</el-button>
    </div>
    <div 
      v-for="session in sessions" 
      :key="session.sessionId"
      class="session-item"
      :class="{ active: currentSessionId === session.sessionId }"
      @click="switchSession(session.sessionId)"
    >
      <div class="session-title">{{ session.title || '新会话' }}</div>
      <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
    </div>
  </div>
</template>
```

### 5.2 Store 适配

```javascript
// stores/chat.js 添加会话管理

// 获取用户会话列表
async function fetchSessions() {
  const res = await chatApi.getSessions()
  if (res.code === 200) {
    sessions.value = res.data
  }
}

// 切换会话
async function switchSession(sessionId) {
  currentSessionId.value = sessionId
  const res = await chatApi.getSessionMessages(sessionId)
  if (res.code === 200) {
    messages.value = res.data.map(msg => ({
      role: msg.role,
      content: msg.content,
      timestamp: msg.timestamp,
      // ... 其他字段
    }))
  }
}

// 创建新会话
function createNewSession() {
  currentSessionId.value = null
  messages.value = []
}
```

---

## 6. 部署配置

### 6.1 Docker Compose

```yaml
# docker-compose.yml 添加 MongoDB 服务
services:
  mongodb:
    image: mongo:7.0
    container_name: patent-mongodb
    restart: always
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: your_password
      MONGO_INITDB_DATABASE: patent_chat
    volumes:
      - mongodb_data:/data/db
      - ./mongo-init.js:/docker-entrypoint-initdb.d/init.js:ro
    networks:
      - patent-network

volumes:
  mongodb_data:
```

### 6.2 初始化脚本

```javascript
// mongo-init.js
db = db.getSiblingDB('patent_chat');

// 创建应用用户
db.createUser({
  user: 'patent_user',
  pwd: 'your_password',
  roles: [{ role: 'readWrite', db: 'patent_chat' }]
});

// 创建索引
db.chat_sessions.createIndex({ "userId": 1, "updatedAt": -1 });
db.chat_sessions.createIndex({ "sessionId": 1 }, { unique: true });
db.chat_messages.createIndex({ "sessionId": 1, "sequence": 1 });
db.chat_messages.createIndex({ "sessionId": 1, "timestamp": -1 });
```

---

## 7. 注意事项

### 7.1 数据一致性

- MongoDB 默认单文档原子性，会话和消息分集合存储时需注意
- 建议使用事务或设计幂等操作

### 7.2 性能优化

- 消息列表查询使用分页，避免一次加载过多数据
- 合理设置 `maxMessages` 窗口大小（建议 20-50）
- 定期归档旧会话数据

### 7.3 安全考虑

- API 层校验用户只能访问自己的会话
- 敏感信息（如 API 密钥）不要存入消息 metadata

### 7.4 TTL 策略（可选）

```javascript
// 自动删除 30 天前的已归档会话
db.chat_sessions.createIndex(
  { "updatedAt": 1 },
  { expireAfterSeconds: 2592000, partialFilterExpression: { status: "archived" } }
);
```

---

## 8. 实施计划

| 阶段 | 任务 | 说明 |
|------|------|------|
| 1 | 部署 MongoDB | Docker 或云服务 |
| 2 | 添加依赖和配置 | pom.xml + application.yml |
| 3 | 实现实体和 Repository | ChatSession + ChatMessage |
| 4 | 实现 MongoChatMemoryRepository | 核心存储逻辑 |
| 5 | 修改 ChatServiceImpl | 集成 MongoDB 存储 |
| 6 | 新增 API 接口 | 会话列表、历史消息 |
| 7 | 前端适配 | 会话管理 UI |
| 8 | 测试和优化 | 性能测试、索引优化 |

---

## 9. 总结

本方案通过引入 MongoDB 存储聊天历史，实现了：

- ✅ **用户级管理**：每个用户独立的会话数据
- ✅ **多会话支持**：用户可创建和切换多个会话
- ✅ **历史持久化**：服务重启不丢失数据
- ✅ **与 Spring AI 无缝集成**：通过自定义 `ChatMemoryRepository`
- ✅ **可扩展性**：文档型存储便于扩展 metadata

MongoDB 的文档模型非常适合存储聊天消息这类半结构化数据，配合 Spring Data MongoDB 可以快速实现功能。

---

## 10. 实现完成记录

### 10.1 已实现的文件

**后端文件：**

| 文件路径 | 说明 |
|---------|------|
| `pom.xml` | 添加了 `spring-boot-starter-data-mongodb` 依赖 |
| `application.yml` | 添加了 MongoDB 连接配置 |
| `model/entity/ChatSession.java` | 聊天会话 MongoDB 实体 |
| `model/entity/ChatMessage.java` | 聊天消息 MongoDB 实体 |
| `repository/ChatSessionRepository.java` | 会话 MongoDB Repository |
| `repository/ChatMessageRepository.java` | 消息 MongoDB Repository |
| `config/MongoChatMemoryRepository.java` | 实现 Spring AI ChatMemoryRepository 接口 |
| `model/vo/ChatSessionVO.java` | 会话 VO |
| `model/vo/ChatMessageVO.java` | 消息 VO |
| `service/ChatService.java` | 添加会话管理接口方法 |
| `service/impl/ChatServiceImpl.java` | 完整实现会话管理功能 |
| `controller/ChatController.java` | 添加会话管理 REST API |

**前端文件：**

| 文件路径 | 说明 |
|---------|------|
| `api/chat.js` | 添加会话管理 API 方法 |
| `stores/chat.js` | 添加会话管理状态和方法 |
| `components/chat/ChatSessionSidebar.vue` | 新增会话列表侧边栏组件 |
| `views/chat/PatentChat.vue` | 集成会话侧边栏 |

**部署文件：**

| 文件路径 | 说明 |
|---------|------|
| `docker-compose.yml` | 添加 MongoDB 服务配置 |
| `mongo-init.js` | MongoDB 初始化脚本（创建索引） |

### 10.2 新增 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/sessions` | 获取用户会话列表 |
| GET | `/api/chat/sessions/{sessionId}/messages` | 获取会话消息历史 |
| DELETE | `/api/chat/sessions/{sessionId}` | 删除会话 |
| PUT | `/api/chat/sessions/{sessionId}/title` | 更新会话标题 |
| PUT | `/api/chat/sessions/{sessionId}/archive` | 归档会话 |

### 10.3 使用说明

1. **启动 MongoDB**：
   - 开发环境：安装并启动本地 MongoDB，默认端口 27017
   - Docker 环境：`docker-compose up -d mongodb`

2. **配置连接**：
   ```yaml
   spring:
     data:
       mongodb:
         uri: mongodb://localhost:27017/patent_chat
   ```

3. **功能特性**：
   - 聊天历史自动持久化到 MongoDB
   - 支持多会话管理（创建、切换、删除、重命名、归档）
   - 会话按用户隔离，权限校验确保数据安全
   - 会话标题从首条用户消息自动生成
   - 与 Spring AI MessageWindowChatMemory 无缝集成
