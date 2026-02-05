# Kibana Dev Tools - 专利索引管理 DSL

> 适用版本: Elasticsearch 8.12+
> 前置条件: 已安装 IK 分词器插件
> 字段命名: 与MySQL数据库保持一致（下划线风格）

---

## 数据库字段映射

| MySQL表 | 字段 | ES字段 | 类型 |
|---------|------|--------|------|
| patent | id | id | keyword |
| patent | publication_no | publication_no | keyword |
| patent | title | title | text + keyword |
| patent | applicant | applicant | keyword + text |
| patent | publication_date | publication_date | date |
| patent | abstract | abstract_text | text |
| patent | source_type | source_type | keyword |
| patent | parse_status | parse_status | keyword |
| patent | created_at | created_at | date |
| patent_entity | entity_name | entities | text + keyword (数组) |
| patent_entity | entity_type | entity_types | keyword (数组) |
| patent_domain | domain_code | domain_codes | keyword (数组) |
| patent_domain | domain_level=1 | domain_section | keyword |

---

## 1. 索引管理

### 1.1 检查ES集群状态

```
GET _cluster/health
```

### 1.2 检查索引是否存在

```
HEAD patent_index
```

### 1.3 删除旧索引（生产环境慎用）

```
DELETE patent_index
```

### 1.4 创建索引（含完整Mapping）

```
PUT patent_index
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
          "filter": ["lowercase"]
        },
        "ik_smart_analyzer": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["lowercase"]
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
      "publication_date": {
        "type": "date",
        "format": "yyyy-MM-dd||yyyy/MM/dd||epoch_millis"
      },
      "domain_codes": {
        "type": "keyword"
      },
      "domain_section": {
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

### 1.5 查看索引信息

```
GET patent_index
```

### 1.6 查看索引Mapping

```
GET patent_index/_mapping
```

### 1.7 查看索引Settings

```
GET patent_index/_settings
```

### 1.8 查看索引统计

```
GET patent_index/_stats
```

---

## 2. IK分词器测试

### 2.1 测试 ik_max_word 分词（细粒度）

```
POST patent_index/_analyze
{
  "analyzer": "ik_max_analyzer",
  "text": "基于深度学习的医疗图像病变检测方法"
}
```

### 2.2 测试 ik_smart 分词（智能）

```
POST patent_index/_analyze
{
  "analyzer": "ik_smart_analyzer",
  "text": "基于深度学习的医疗图像病变检测方法"
}
```

---

## 3. 文档操作

### 3.1 插入测试文档

```
POST patent_index/_doc/1
{
  "id": "1",
  "publication_no": "CN123456789A",
  "title": "一种基于深度学习的CT图像病变检测方法",
  "abstract_text": "本发明公开了一种基于深度学习的CT图像病变检测方法，采用卷积神经网络对医疗影像进行分析，能够自动识别病变区域，提高诊断效率。",
  "applicant": "华为技术有限公司",
  "publication_date": "2024-06-15",
  "domain_codes": ["G06T7/00", "G06N3/08"],
  "domain_section": "G",
  "entities": ["深度学习", "卷积神经网络", "CT图像", "病变检测", "医疗影像"],
  "entity_types": ["METHOD", "COMPONENT", "PRODUCT", "APPLICATION"],
  "parse_status": "SUCCESS",
  "source_type": "FILE",
  "created_at": "2024-06-20T10:30:00",
  "updated_at": "2024-06-20T10:30:00"
}
```

### 3.2 批量插入文档

```
POST _bulk
{ "index": { "_index": "patent_index", "_id": "2" } }
{ "id": "2", "publication_no": "CN987654321B", "title": "一种智能语音识别系统及方法", "abstract_text": "本发明涉及一种基于Transformer架构的智能语音识别系统，支持多语言实时转录。", "applicant": "阿里巴巴集团", "publication_date": "2024-05-20", "domain_codes": ["G10L15/00"], "domain_section": "G", "entities": ["语音识别", "Transformer", "实时转录"], "entity_types": ["PRODUCT", "METHOD"], "parse_status": "SUCCESS", "source_type": "FILE", "created_at": "2024-05-25T09:00:00" }
{ "index": { "_index": "patent_index", "_id": "3" } }
{ "id": "3", "publication_no": "CN111222333A", "title": "一种新型锂电池正极材料及制备方法", "abstract_text": "本发明公开了一种高能量密度的锂电池正极材料，采用纳米级颗粒结构，显著提升电池循环寿命。", "applicant": "宁德时代", "publication_date": "2024-04-10", "domain_codes": ["H01M4/00", "H01M10/05"], "domain_section": "H", "entities": ["锂电池", "正极材料", "纳米颗粒"], "entity_types": ["PRODUCT", "MATERIAL"], "parse_status": "SUCCESS", "source_type": "FILE", "created_at": "2024-04-15T14:00:00" }
```

### 3.3 查看文档

```
GET patent_index/_doc/1
```

### 3.4 更新文档

```
POST patent_index/_update/1
{
  "doc": {
    "parse_status": "SUCCESS",
    "updated_at": "2024-06-21T10:00:00"
  }
}
```

### 3.5 删除文档

```
DELETE patent_index/_doc/1
```

### 3.6 查看文档总数

```
GET patent_index/_count
```

---

## 4. 快速检索

### 4.1 基础多字段检索

```
POST patent_index/_search
{
  "query": {
    "multi_match": {
      "query": "深度学习图像识别",
      "fields": [
        "title^4",
        "abstract_text^2",
        "entities^3"
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
      "title": { "number_of_fragments": 0 },
      "abstract_text": { "number_of_fragments": 3, "fragment_size": 150 },
      "entities": { "number_of_fragments": 0 }
    }
  },
  "_source": ["id", "publication_no", "title", "abstract_text", "applicant", "publication_date", "domain_codes", "entities", "parse_status"],
  "from": 0,
  "size": 10,
  "sort": [
    { "_score": { "order": "desc" } },
    { "publication_date": { "order": "desc", "missing": "_last" } }
  ]
}
```

### 4.2 带短语提升的快速检索

```
POST patent_index/_search
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
            "fields": ["title^5", "abstract_text^3", "entities^3"],
            "type": "phrase",
            "slop": 2,
            "boost": 2
          }
        }
      ],
      "minimum_should_match": 1,
      "filter": [
        { "term": { "parse_status": "SUCCESS" } }
      ]
    }
  },
  "highlight": {
    "pre_tags": ["<em class=\"highlight\">"],
    "post_tags": ["</em>"],
    "fields": {
      "title": {},
      "abstract_text": { "number_of_fragments": 2, "fragment_size": 200 }
    }
  },
  "_source": ["id", "publication_no", "title", "abstract_text", "applicant", "publication_date", "domain_codes", "entities"],
  "from": 0,
  "size": 10
}
```

---

## 5. 高级检索

### 5.1 多条件组合检索

```
POST patent_index/_search
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
              "query": "卷积神经网络",
              "boost": 1.5
            }
          }
        }
      ],
      "filter": [
        { "term": { "parse_status": "SUCCESS" } },
        { "terms": { "domain_section": ["G", "H"] } },
        {
          "range": {
            "publication_date": {
              "gte": "2020-01-01",
              "lte": "2026-12-31"
            }
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
  "_source": ["id", "publication_no", "title", "abstract_text", "applicant", "publication_date", "domain_codes", "domain_section", "entities", "entity_types"],
  "from": 0,
  "size": 20,
  "sort": [
    { "_score": { "order": "desc" } },
    { "publication_date": { "order": "desc" } }
  ]
}
```

### 5.2 申请人精确匹配 + 领域前缀匹配

```
POST patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "图像识别" } }
      ],
      "filter": [
        { "term": { "applicant": "华为技术有限公司" } },
        { "prefix": { "domain_codes": "G06T" } },
        { "terms": { "entity_types": ["METHOD", "PRODUCT"] } },
        {
          "range": {
            "publication_date": { "gte": "2023-01-01" }
          }
        }
      ]
    }
  },
  "_source": ["id", "publication_no", "title", "applicant", "publication_date", "domain_codes", "entities", "entity_types"],
  "from": 0,
  "size": 10
}
```

### 5.3 申请人模糊匹配

```
POST patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "applicant.text": "华为"
          }
        }
      ],
      "filter": [
        { "term": { "parse_status": "SUCCESS" } }
      ]
    }
  },
  "_source": ["id", "publication_no", "title", "applicant"],
  "from": 0,
  "size": 10
}
```

### 5.4 实体关键词检索

```
POST patent_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "entities": "神经网络 深度学习"
          }
        }
      ],
      "filter": [
        { "terms": { "entity_types": ["METHOD", "COMPONENT"] } }
      ]
    }
  },
  "_source": ["id", "title", "entities", "entity_types"],
  "from": 0,
  "size": 10
}
```

### 5.5 专利号精确查询

```
POST patent_index/_search
{
  "query": {
    "term": {
      "publication_no": "CN123456789A"
    }
  },
  "_source": ["id", "publication_no", "title", "applicant", "publication_date"]
}
```

---

## 6. 深度分页（search_after）

### 6.1 首次查询

```
POST patent_index/_search
{
  "query": {
    "match": { "title": "深度学习" }
  },
  "_source": ["id", "publication_no", "title", "publication_date"],
  "size": 20,
  "sort": [
    { "publication_date": { "order": "desc", "missing": "_last" } },
    { "id": { "order": "asc" } }
  ]
}
```

### 6.2 后续分页（使用上次结果的sort值）

```
POST patent_index/_search
{
  "query": {
    "match": { "title": "深度学习" }
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

---

## 7. 聚合统计

### 7.1 领域分布统计

```
POST patent_index/_search
{
  "query": {
    "match": { "title": "深度学习" }
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
        "domain_codes_detail": {
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
    }
  }
}
```

### 7.2 申请人排行统计

```
POST patent_index/_search
{
  "query": {
    "match_all": {}
  },
  "size": 0,
  "aggs": {
    "top_applicants": {
      "terms": {
        "field": "applicant",
        "size": 10
      }
    }
  }
}
```

### 7.3 实体类型分布

```
POST patent_index/_search
{
  "query": {
    "bool": {
      "filter": { "term": { "domain_section": "G" } }
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

### 7.4 申请人专利趋势

```
POST patent_index/_search
{
  "query": {
    "term": { "applicant": "华为技术有限公司" }
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
    }
  }
}
```

### 7.5 解析状态统计

```
POST patent_index/_search
{
  "size": 0,
  "aggs": {
    "parse_status_stats": {
      "terms": {
        "field": "parse_status"
      }
    },
    "source_type_stats": {
      "terms": {
        "field": "source_type"
      }
    }
  }
}
```

---

## 8. 索引维护

### 8.1 强制刷新索引

```
POST patent_index/_refresh
```

### 8.2 强制合并段（优化查询性能）

```
POST patent_index/_forcemerge?max_num_segments=1
```

### 8.3 清空索引数据（保留结构）

```
POST patent_index/_delete_by_query
{
  "query": {
    "match_all": {}
  }
}
```

### 8.4 更新索引设置

```
PUT patent_index/_settings
{
  "index": {
    "refresh_interval": "30s",
    "number_of_replicas": 2
  }
}
```

### 8.5 关闭索引

```
POST patent_index/_close
```

### 8.6 打开索引

```
POST patent_index/_open
```

---

## 9. 调试与排错

### 9.1 解释查询评分

```
POST patent_index/_explain/1
{
  "query": {
    "match": {
      "title": "深度学习"
    }
  }
}
```

### 9.2 验证查询语法

```
POST patent_index/_validate/query?explain=true
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "深度学习" } }
      ],
      "filter": [
        { "term": { "parse_status": "SUCCESS" } }
      ]
    }
  }
}
```

### 9.3 分析查询性能

```
POST patent_index/_search?explain=true
{
  "profile": true,
  "query": {
    "match": {
      "title": "深度学习"
    }
  },
  "size": 1
}
```

---

## 附录：字段说明

| 字段 | 类型 | 说明 | 对应数据库字段 |
|------|------|------|----------------|
| `id` | keyword | 专利ID | patent.id |
| `publication_no` | keyword | 公开号/专利号 | patent.publication_no |
| `title` | text | 专利名称（IK分词） | patent.title |
| `abstract_text` | text | 专利摘要（IK分词） | patent.abstract |
| `applicant` | keyword | 申请人（精确+模糊） | patent.applicant |
| `publication_date` | date | 公开日期 | patent.publication_date |
| `domain_codes` | keyword[] | IPC分类码列表 | patent_domain.domain_code |
| `domain_section` | keyword | IPC部（level=1） | patent_domain.domain_code |
| `entities` | text[] | 技术实体列表 | patent_entity.entity_name |
| `entity_types` | keyword[] | 实体类型列表 | patent_entity.entity_type |
| `parse_status` | keyword | 解析状态 | patent.parse_status |
| `source_type` | keyword | 来源类型 | patent.source_type |
| `created_at` | date | 创建时间 | patent.created_at |
| `updated_at` | date | 更新时间 | patent.updated_at |

---

**文档版本**: v1.1  
**更新日期**: 2026-02-02  
**适用版本**: Elasticsearch 8.12+
