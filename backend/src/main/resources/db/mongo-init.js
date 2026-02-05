// MongoDB 初始化脚本
// 创建聊天历史数据库和索引

// 切换到 patent_chat 数据库
db = db.getSiblingDB('patent_chat');

// 创建应用用户（可选，如果需要单独的应用用户）
// db.createUser({
//   user: 'patent_user',
//   pwd: 'patent_pwd',
//   roles: [{ role: 'readWrite', db: 'patent_chat' }]
// });

// 创建 chat_sessions 集合索引
db.chat_sessions.createIndex({ "userId": 1, "updatedAt": -1 }, { name: "user_updated_idx" });
db.chat_sessions.createIndex({ "sessionId": 1 }, { unique: true, name: "session_id_idx" });
db.chat_sessions.createIndex({ "userId": 1, "status": 1 }, { name: "user_status_idx" });

// 创建 chat_messages 集合索引
db.chat_messages.createIndex({ "sessionId": 1, "sequence": 1 }, { name: "session_sequence_idx" });
db.chat_messages.createIndex({ "sessionId": 1, "timestamp": -1 }, { name: "session_time_idx" });
db.chat_messages.createIndex({ "userId": 1, "timestamp": -1 }, { name: "user_time_idx" });

// 可选：为已归档会话创建 TTL 索引（30天后自动删除）
// db.chat_sessions.createIndex(
//   { "updatedAt": 1 },
//   { 
//     expireAfterSeconds: 2592000, // 30天
//     partialFilterExpression: { status: "archived" },
//     name: "archived_ttl_idx"
//   }
// );

print("MongoDB 初始化完成 - patent_chat 数据库索引已创建");
