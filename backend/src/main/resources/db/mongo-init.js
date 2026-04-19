// MongoDB 初始化脚本 - 专利匹配系统聊天模块
// 自动创建数据库、集合、索引、权限

// 1. 切换到专利聊天数据库
db = db.getSiblingDB('patent_chat');

// 2. 创建专用账号（Docker 启动时自动授权，必须加！）
db.createUser({
  user: "root",
  pwd: "280054",
  roles: [
    { role: "readWrite", db: "patent_chat" }
  ]
});

// 3. 会话集合索引（保证查询速度）
db.chat_sessions.createIndex({ "userId": 1, "updatedAt": -1 });
db.chat_sessions.createIndex({ "sessionId": 1 }, { unique: true });
db.chat_sessions.createIndex({ "userId": 1, "status": 1 });

// 4. 消息集合索引（保证聊天加载速度）
db.chat_messages.createIndex({ "sessionId": 1, "sequence": 1 });
db.chat_messages.createIndex({ "sessionId": 1, "timestamp": -1 });
db.chat_messages.createIndex({ "userId": 1, "timestamp": -1 });

// 5. 自动清理已归档会话（30天自动删除）
db.chat_sessions.createIndex(
  { "updatedAt": 1 },
  {
    expireAfterSeconds: 2592000, // 30天
    partialFilterExpression: { status: "archived" }
  }
);

print("✅ MongoDB 初始化完成：数据库 patent_chat 已就绪");