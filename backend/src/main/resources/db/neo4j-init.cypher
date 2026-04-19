// Neo4j 约束与索引初始化脚本
// 在 Neo4j Browser 或 Cypher Shell 中执行

// ==================== 唯一性约束 ====================

CREATE CONSTRAINT patent_pubno_unique IF NOT EXISTS
FOR (p:Patent)
REQUIRE p.publicationNo IS NODE UNIQUE;

CREATE CONSTRAINT ipc_code_unique IF NOT EXISTS
FOR (i:IPC)
REQUIRE i.ipcCode IS NODE UNIQUE;

CREATE CONSTRAINT entity_name_unique IF NOT EXISTS
FOR (e:Entity)
REQUIRE e.name IS NODE UNIQUE;

CREATE CONSTRAINT applicant_name_unique IF NOT EXISTS
FOR (a:Applicant)
REQUIRE a.applicantName IS NODE UNIQUE;

CREATE CONSTRAINT concept_name_unique IF NOT EXISTS
FOR (c:Concept)
REQUIRE c.name IS NODE UNIQUE;

// ==================== 全文索引 ====================

CREATE FULLTEXT INDEX patent_fulltext IF NOT EXISTS
FOR (p:Patent)
ON EACH [p.title, p.abstractText];

CREATE FULLTEXT INDEX entity_fulltext IF NOT EXISTS
FOR (e:Entity)
ON EACH [e.name, e.alias];

// ==================== 普通索引（加速查询） ====================

CREATE INDEX patent_applicant_idx IF NOT EXISTS
FOR (p:Patent)
ON (p.applicant);

CREATE INDEX patent_source_type_idx IF NOT EXISTS
FOR (p:Patent)
ON (p.sourceType);

CREATE INDEX ipc_level_idx IF NOT EXISTS
FOR (i:IPC)
ON (i.level);

CREATE INDEX entity_type_idx IF NOT EXISTS
FOR (e:Entity)
ON (e.entityType);
