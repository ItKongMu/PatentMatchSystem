# Elasticsearch 检索方案设计文档

> 目标版本：Elasticsearch 8.12+
> 中文分词：IK分词器 8.12.0

---

## 目录

1. [索引设计](#1-索引设计)
2. [快速检索方案](#2-快速检索方案)
3. [高级检索方案](#3-高级检索方案)
4. [深度分页方案](#4-深度分页方案)
5. [聚合分析](#5-聚合分析)
6. [性能优化](#6-性能优化)

---

## 1. 索引设计

### 1.1 创建索引（含IK分词器配置）

```json
PUT /patent_index
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "5s",
    "analysis": {
      "analyzer": {
        "ik_max_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase", "patent_synonym"]
        },
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["lowercase"]
        }
      },
      "filter": {
        "patent_synonym": {
          "type": "synonym",
          "synonyms": [
            "人工智能,AI,机器学习,深度学习",
            "专利,发明,创新",
            "算法,方法,技术"
          ]
        }
      }
    },
    "index": {
      "max_result_window": 10000
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword"
      },
      "publication_no": {
        "type": "keyword"
      },
      "title": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 512
          },
          "suggest": {
            "type": "completion",
            "analyzer": "ik_max_analyzer"
          }
        }
      },
      "abstract_text": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "search_analyzer": "ik_smart_analyzer"
      },
      "applicant": {
        "type": "keyword",
        "fields": {
          "text": {
            "type": "text",
            "analyzer": "ik_smart_analyzer"
          }
        }
      },
      "inventor": {
        "type": "keyword"
      },
      "publication_date": {
        "type": "date",
        "format": "yyyy-MM-dd||yyyy/MM/dd||epoch_millis"
      },
      "apply_date": {
        "type": "date",
        "format": "yyyy-MM-dd||yyyy/MM/dd||epoch_millis"
      },
      "domain_codes": {
        "type": "keyword"
      },
      "domain_section": {
        "type": "keyword"
      },
      "domain_class": {
        "type": "keyword"
      },
      "entities": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "search_analyzer": "ik_smart_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "entity_types": {
        "type": "keyword"
      },
      "keywords": {
        "type": "text",
        "analyzer": "ik_max_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "parse_status": {
        "type": "keyword"
      },
      "source_type": {
        "type": "keyword"
      },
      "created_at": {
        "type": "date"
      },
      "updated_at": {
        "type": "date"
      }
    }
  }
}
```

### 1.2 字段设计说明

| 字段 | 类型 | 分词 | 用途 |
|------|------|------|------|
| `id` | keyword | 否 | 主键，精确匹配 |
| `publication_no` | keyword | 否 | 专利号，精确匹配 |
| `title` | text + keyword | IK分词 | 全文检索 + 精确匹配 + 自动补全 |
| `abstract_text` | text | IK分词 | 摘要全文检索 |
| `applicant` | keyword + text | 混合 | 精确过滤 + 模糊搜索 |
| `domain_codes` | keyword | 否 | IPC分类码精确过滤 |
| `entities` | text + keyword | IK分词 | 实体检索 + 聚合 |
| `entity_types` | keyword | 否 | 实体类型过滤 |
| `keywords` | text + keyword | IK分词 | 关键词检索 + 聚合 |

---

## 2. 快速检索方案

### 2.1 基础快速检索DSL

```json
POST /patent_index/_search
{
  "query": {
    "multi_match": {
      "query": "深度学习图像识别",
      "fields": [
        "title^4",
        "abstract_text^2",
        "entities^3",
        "keywords^2"
      ],
      "type": "best_fields",
      "operator": "or",
      "minimum_should_match": "30%",
      "fuzziness": "AUTO"
    }
  },
  "highlight": {
    "pre_tags": ["<em class=\"highlight\">"],
    "post_tags": ["</em>"],
    "fields": {
      "title": {
        "number_of_fragments": 0
      },
      "abstract_text": {
        "number_of_fragments": 3,
        "fragment_size": 150
      },
      "entities": {
        "number_of_fragments": 0
      }
    }
  },
  "_source": {
    "includes": ["id", "publication_no", "title", "abstract_text", "applicant", 
                 "publication_date", "domain_codes", "domain_section", 
                 "entities", "entity_types", "parse_status"]
  },
  "from": 0,
  "size": 10,
  "sort": [
    { "_score": { "order": "desc" } },
    { "publication_date": { "order": "desc", "missing": "_last" } }
  ]
}
```

### 2.2 带短语匹配的快速检索（提高精确度）

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "should": [
        {
          "multi_match": {
            "query": "深度学习图像识别",
            "fields": ["title^4", "abstract_text^2", "entities^3"],
            "type": "best_fields",
            "operator": "or",
            "minimum_should_match": "30%"
          }
        },
        {
          "multi_match": {
            "query": "深度学习图像识别",
            "fields": ["title^5", "abstract_text^3"],
            "type": "phrase",
            "slop": 2,
            "boost": 2
          }
        }
      ],
      "minimum_should_match": 1,
      "filter": [
        {
          "term": {
            "parse_status": "SUCCESS"
          }
        }
      ]
    }
  },
  "highlight": {
    "pre_tags": ["<em class=\"highlight\">"],
    "post_tags": ["</em>"],
    "fields": {
      "title": {},
      "abstract_text": {
        "number_of_fragments": 2,
        "fragment_size": 200
      }
    }
  },
  "_source": {
    "includes": ["id", "publication_no", "title", "abstract_text", "applicant",
                 "publication_date", "domain_codes", "entities", "parse_status"]
  },
  "from": 0,
  "size": 10
}
```

### 2.3 带自动补全的快速检索

```json
POST /patent_index/_search
{
  "suggest": {
    "title_suggest": {
      "prefix": "深度学",
      "completion": {
        "field": "title.suggest",
        "size": 5,
        "skip_duplicates": true,
        "fuzzy": {
          "fuzziness": "AUTO"
        }
      }
    }
  }
}
```

### 2.4 相关性评分优化（Function Score）

```json
POST /patent_index/_search
{
  "query": {
    "function_score": {
      "query": {
        "multi_match": {
          "query": "深度学习图像识别",
          "fields": ["title^4", "abstract_text^2", "entities^3", "keywords^2"],
          "type": "best_fields"
        }
      },
      "functions": [
        {
          "filter": {
            "term": { "domain_section": "G" }
          },
          "weight": 1.5
        },
        {
          "gauss": {
            "publication_date": {
              "origin": "now",
              "scale": "365d",
              "offset": "30d",
              "decay": 0.5
            }
          },
          "weight": 1.2
        },
        {
          "field_value_factor": {
            "field": "entity_count",
            "factor": 1.1,
            "modifier": "log1p",
            "missing": 1
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  },
  "highlight": {
    "fields": {
      "title": {},
      "abstract_text": { "fragment_size": 150 }
    }
  },
  "_source": {
    "includes": ["id", "publication_no", "title", "abstract_text", "applicant",
                 "publication_date", "domain_codes", "entities"]
  },
  "from": 0,
  "size": 10
}
```

---

## 3. 高级检索方案

### 3.1 多条件组合检索

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "title": {
              "query": "深度学习",
              "operator": "and"
            }
          }
        }
      ],
      "should": [
        {
          "match": {
            "abstract_text": {
              "query": "图像处理 神经网络",
              "minimum_should_match": "50%"
            }
          }
        },
        {
          "match": {
            "entities": {
              "query": "卷积神经网络 CNN",
              "boost": 1.5
            }
          }
        }
      ],
      "filter": [
        {
          "term": {
            "parse_status": "SUCCESS"
          }
        },
        {
          "terms": {
            "domain_section": ["G", "H"]
          }
        },
        {
          "range": {
            "publication_date": {
              "gte": "2020-01-01",
              "lte": "2026-12-31"
            }
          }
        }
      ],
      "must_not": [
        {
          "term": {
            "source_type": "DELETED"
          }
        }
      ],
      "minimum_should_match": 1
    }
  },
  "highlight": {
    "pre_tags": ["<em class=\"highlight\">"],
    "post_tags": ["</em>"],
    "fields": {
      "title": { "number_of_fragments": 0 },
      "abstract_text": { "number_of_fragments": 2, "fragment_size": 200 },
      "entities": { "number_of_fragments": 0 }
    }
  },
  "_source": {
    "includes": ["id", "publication_no", "title", "abstract_text", "applicant",
                 "publication_date", "domain_codes", "domain_section", 
                 "entities", "entity_types"]
  },
  "from": 0,
  "size": 20,
  "sort": [
    { "_score": { "order": "desc" } },
    { "publication_date": { "order": "desc" } }
  ]
}
```

### 3.2 精确字段过滤检索

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "title": "图像识别"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "applicant": "华为技术有限公司"
          }
        },
        {
          "prefix": {
            "domain_codes": "G06T"
          }
        },
        {
          "terms": {
            "entity_types": ["METHOD", "PRODUCT"]
          }
        },
        {
          "range": {
            "publication_date": {
              "gte": "2023-01-01"
            }
          }
        }
      ]
    }
  },
  "_source": {
    "includes": ["id", "publication_no", "title", "applicant", "publication_date",
                 "domain_codes", "entities", "entity_types"]
  },
  "from": 0,
  "size": 10
}
```

### 3.3 IPC分类层级检索

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "abstract_text": "数据处理"
          }
        }
      ],
      "filter": [
        {
          "bool": {
            "should": [
              { "term": { "domain_section": "G" } },
              { "prefix": { "domain_codes": "G06F" } },
              { "wildcard": { "domain_codes": "G06F16*" } }
            ],
            "minimum_should_match": 1
          }
        }
      ]
    }
  },
  "_source": ["id", "publication_no", "title", "domain_codes", "domain_section"],
  "from": 0,
  "size": 20
}
```

### 3.4 实体类型组合检索

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "entities": "神经网络"
          }
        }
      ],
      "filter": [
        {
          "terms": {
            "entity_types": ["METHOD", "COMPONENT"]
          }
        },
        {
          "script": {
            "script": {
              "source": "doc['entity_types'].length >= params.min_types",
              "params": {
                "min_types": 2
              }
            }
          }
        }
      ]
    }
  },
  "aggs": {
    "entity_type_distribution": {
      "terms": {
        "field": "entity_types",
        "size": 10
      }
    }
  },
  "_source": ["id", "title", "entities", "entity_types"],
  "from": 0,
  "size": 10
}
```

### 3.5 模糊匹配与通配符检索

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "should": [
        {
          "fuzzy": {
            "title": {
              "value": "深度学习",
              "fuzziness": "AUTO",
              "prefix_length": 2
            }
          }
        },
        {
          "wildcard": {
            "publication_no": {
              "value": "CN2024*",
              "boost": 1.0
            }
          }
        },
        {
          "regexp": {
            "applicant": {
              "value": ".*科技.*公司",
              "flags": "ALL"
            }
          }
        }
      ],
      "minimum_should_match": 1
    }
  },
  "_source": ["id", "publication_no", "title", "applicant"],
  "from": 0,
  "size": 10
}
```

### 3.6 跨字段联合检索（Cross Fields）

```json
POST /patent_index/_search
{
  "query": {
    "multi_match": {
      "query": "华为 图像处理 深度学习",
      "fields": ["title", "abstract_text", "applicant.text", "entities"],
      "type": "cross_fields",
      "operator": "and",
      "minimum_should_match": "75%"
    }
  },
  "highlight": {
    "fields": {
      "title": {},
      "abstract_text": {},
      "applicant.text": {}
    }
  },
  "_source": ["id", "publication_no", "title", "applicant", "abstract_text"],
  "from": 0,
  "size": 10
}
```

---

## 4. 深度分页方案

### 4.1 使用 search_after（推荐）

**首次查询**：

```json
POST /patent_index/_search
{
  "query": {
    "match": {
      "title": "深度学习"
    }
  },
  "_source": ["id", "publication_no", "title", "publication_date"],
  "size": 20,
  "sort": [
    { "publication_date": { "order": "desc", "missing": "_last" } },
    { "id": { "order": "asc" } }
  ]
}
```

**后续分页查询**（使用上次结果的最后一条记录的sort值）：

```json
POST /patent_index/_search
{
  "query": {
    "match": {
      "title": "深度学习"
    }
  },
  "_source": ["id", "publication_no", "title", "publication_date"],
  "size": 20,
  "sort": [
    { "publication_date": { "order": "desc", "missing": "_last" } },
    { "id": { "order": "asc" } }
  ],
  "search_after": ["2024-06-15", "12345"]
}
```

### 4.2 使用 Point in Time (PIT) + search_after

**创建PIT**：

```json
POST /patent_index/_pit?keep_alive=5m
```

**使用PIT查询**：

```json
POST /_search
{
  "query": {
    "match": {
      "title": "深度学习"
    }
  },
  "pit": {
    "id": "YOUR_PIT_ID",
    "keep_alive": "5m"
  },
  "size": 20,
  "sort": [
    { "publication_date": { "order": "desc" } },
    { "_shard_doc": { "order": "asc" } }
  ],
  "search_after": ["2024-06-15", 12345]
}
```

**删除PIT**：

```json
DELETE /_pit
{
  "id": "YOUR_PIT_ID"
}
```

### 4.3 Scroll API（仅用于数据导出，不推荐用于用户分页）

```json
POST /patent_index/_search?scroll=5m
{
  "query": {
    "match_all": {}
  },
  "size": 1000,
  "sort": ["_doc"]
}

POST /_search/scroll
{
  "scroll": "5m",
  "scroll_id": "YOUR_SCROLL_ID"
}
```

---

## 5. 聚合分析

### 5.1 领域分布统计

```json
POST /patent_index/_search
{
  "query": {
    "match": {
      "title": "深度学习"
    }
  },
  "size": 0,
  "aggs": {
    "domain_section_stats": {
      "terms": {
        "field": "domain_section",
        "size": 10,
        "order": { "_count": "desc" }
      },
      "aggs": {
        "domain_codes": {
          "terms": {
            "field": "domain_codes",
            "size": 5
          }
        }
      }
    },
    "publication_year_trend": {
      "date_histogram": {
        "field": "publication_date",
        "calendar_interval": "year",
        "format": "yyyy",
        "min_doc_count": 0
      }
    },
    "top_applicants": {
      "terms": {
        "field": "applicant",
        "size": 10
      }
    }
  }
}
```

### 5.2 实体类型分布

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "filter": {
        "term": { "domain_section": "G" }
      }
    }
  },
  "size": 0,
  "aggs": {
    "entity_type_stats": {
      "terms": {
        "field": "entity_types",
        "size": 10
      }
    },
    "entity_keyword_cloud": {
      "terms": {
        "field": "entities.keyword",
        "size": 50
      }
    }
  }
}
```

### 5.3 申请人专利趋势

```json
POST /patent_index/_search
{
  "query": {
    "term": {
      "applicant": "华为技术有限公司"
    }
  },
  "size": 0,
  "aggs": {
    "monthly_trend": {
      "date_histogram": {
        "field": "publication_date",
        "calendar_interval": "month",
        "format": "yyyy-MM"
      }
    },
    "domain_distribution": {
      "terms": {
        "field": "domain_codes",
        "size": 20
      }
    },
    "avg_entities_per_patent": {
      "avg": {
        "script": {
          "source": "doc['entities.keyword'].length"
        }
      }
    }
  }
}
```

---

## 6. 性能优化

### 6.1 查询优化建议

| 优化点 | 说明 |
|--------|------|
| **使用filter替代must** | filter子句不参与评分，可利用缓存 |
| **避免前导通配符** | `*keyword` 会导致全索引扫描 |
| **限制返回字段** | 使用 `_source` filtering 减少数据传输 |
| **合理设置分片数** | 小数据集1-3个分片，大数据集按需扩展 |
| **使用routing** | 相同申请人的专利路由到同一分片 |

### 6.2 索引优化配置

```json
PUT /patent_index/_settings
{
  "index": {
    "refresh_interval": "30s",
    "number_of_replicas": 1,
    "translog": {
      "durability": "async",
      "sync_interval": "30s"
    }
  }
}
```

### 6.3 查询缓存优化

```json
POST /patent_index/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "parse_status": "SUCCESS"
          }
        }
      ]
    }
  },
  "request_cache": true
}
```

### 6.4 批量索引优化

```json
POST /_bulk
{ "index": { "_index": "patent_index", "_id": "1" } }
{ "publication_no": "CN123456A", "title": "一种深度学习方法", ... }
{ "index": { "_index": "patent_index", "_id": "2" } }
{ "publication_no": "CN123457A", "title": "图像识别系统", ... }
```

---

## 附录：错误处理与降级

### A.1 查询超时处理

```json
POST /patent_index/_search
{
  "timeout": "10s",
  "query": {
    "match": {
      "title": "深度学习"
    }
  }
}
```

### A.2 降级到MySQL的条件

- ES连接超时（>5s）
- ES集群状态为RED
- 查询结果为空且有MySQL备份数据

---

**文档版本**：v1.0  
**编写日期**：2026年2月2日  
**适用版本**：Elasticsearch 8.12+
