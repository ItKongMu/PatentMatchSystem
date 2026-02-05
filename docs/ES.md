# Elasticsearch 8.12.2 升级部署指南

## 一、环境说明

| 组件          | 原版本 | 升级版本 |
| ------------- | ------ | -------- |
| Elasticsearch | 7.12.1 | 8.12.2   |
| Kibana        | 7.12.1 | 8.12.2   |
| IK 分词器     | 7.12.1 | 8.12.2   |

**原部署配置：**

- 网络：heima
- 数据卷：es-data, es-plugins
- 插件目录：`/var/lib/docker/volumes/es-plugins/_data`

---

## 二、升级前准备

### 2.1 备份数据

```bash
# 备份数据卷
sudo cp -r /var/lib/docker/volumes/es-data /var/lib/docker/volumes/es-data-backup
sudo cp -r /var/lib/docker/volumes/es-plugins /var/lib/docker/volumes/es-plugins-backup

# 查看现有索引
curl -X GET "http://localhost:9200/_cat/indices?v"
```

### 2.2 停止并删除旧容器

```bash
# 停止容器
docker stop es kibana

# 删除容器
docker rm es kibana

# 确认删除
docker ps -a | grep -E "es|kibana"
```

### 2.3 清理旧数据

```bash
# 删除旧的 IK 分词器
sudo rm -rf /var/lib/docker/volumes/es-plugins/_data/*

# 清空旧数据（8.x 不兼容 7.x 数据格式）
sudo rm -rf /var/lib/docker/volumes/es-data/_data/*
```

---

## 三、拉取新版本镜像

```bash
# 拉取 ES 8.12.2
docker pull elasticsearch:8.12.2

# 拉取 Kibana 8.12.2
docker pull kibana:8.12.2

# 确认镜像
docker images | grep -E "elasticsearch|kibana"
```

---

## 四、IK 分词器离线安装

### 4.1 下载 IK 分词器

在有网络的机器上下载 IK 分词器 8.12.2：

**下载地址（任选其一）：**

- GitHub：https://github.com/infinilabs/analysis-ik/releases/download/v8.12.2/elasticsearch-analysis-ik-8.12.2.zip
- 镜像站：https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.12.2.zip

### 4.2 上传并安装

```bash
# 1. 将下载的 zip 文件上传到虚拟机（使用 scp、xftp 等工具）
# 例如上传到 /tmp 目录

# 2. 进入插件目录
cd /var/lib/docker/volumes/es-plugins/_data

# 3. 创建 ik 目录
sudo mkdir -p ik

# 4. 解压 IK 分词器到 ik 目录
sudo unzip /tmp/elasticsearch-analysis-ik-8.12.2.zip -d ik/

# 5. 删除 zip 文件（可选）
rm -f /tmp/elasticsearch-analysis-ik-8.12.2.zip
```

### 4.3 验证目录结构

```bash
ls -la /var/lib/docker/volumes/es-plugins/_data/ik/
```

**正确的目录结构：**

```
/var/lib/docker/volumes/es-plugins/_data/ik/
├── commons-codec-1.9.jar
├── commons-logging-1.2.jar
├── config/
│   ├── extra_main.dic
│   ├── extra_single_word.dic
│   ├── extra_single_word_full.dic
│   ├── extra_single_word_low_freq.dic
│   ├── extra_stopword.dic
│   ├── IKAnalyzer.cfg.xml
│   ├── main.dic
│   ├── preposition.dic
│   ├── quantifier.dic
│   ├── stopword.dic
│   ├── suffix.dic
│   └── surname.dic
├── elasticsearch-analysis-ik-8.12.2.jar
├── httpclient-4.5.2.jar
├── httpcore-4.4.4.jar
├── plugin-descriptor.properties
└── plugin-security.policy
```

### 4.4 设置目录权限

```bash
# ES 8.x 容器使用 uid=1000 用户运行
sudo chown -R 1000:1000 /var/lib/docker/volumes/es-data/_data
sudo chown -R 1000:1000 /var/lib/docker/volumes/es-plugins/_data
sudo chmod -R 755 /var/lib/docker/volumes/es-plugins/_data
```

---

## 五、启动 Elasticsearch 8.12.2

```bash
docker run -d \
  --name es \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "xpack.security.enrollment.enabled=false" \
  -e "xpack.security.http.ssl.enabled=false" \
  -e "xpack.security.transport.ssl.enabled=false" \
  -v es-data:/usr/share/elasticsearch/data \
  -v es-plugins:/usr/share/elasticsearch/plugins \
  --privileged \
  --network heima \
  -p 9200:9200 \
  -p 9300:9300 \
  elasticsearch:8.12.2
```

**参数说明：**

| 参数                               | 说明                     |
| ---------------------------------- | ------------------------ |
| `ES_JAVA_OPTS=-Xms512m -Xmx512m` | JVM 堆内存设置           |
| `discovery.type=single-node`     | 单节点模式               |
| `xpack.security.enabled=false`   | 关闭安全认证（开发环境） |
| `--network heima`                | 使用 heima 网络          |
| `-v es-data`                     | 数据持久化               |
| `-v es-plugins`                  | 插件目录挂载             |

---

## 六、验证 Elasticsearch

### 6.1 查看启动日志

```bash
# 等待约 30-60 秒后查看日志
docker logs es

# 实时查看日志
docker logs -f es
```

### 6.2 测试 ES 连接

```bash
curl http://localhost:9200
```

**预期输出：**

```json
{
  "name" : "xxxxx",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "xxxxx",
  "version" : {
    "number" : "8.12.2",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "xxxxx",
    "build_date" : "2024-02-05T10:03:47.129031219Z",
    "build_snapshot" : false,
    "lucene_version" : "9.9.2",
    "minimum_wire_compatibility_version" : "7.17.0",
    "minimum_index_compatibility_version" : "7.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

### 6.3 检查 IK 插件

```bash
curl http://localhost:9200/_cat/plugins?v
```

**预期输出：**

```
name    component    version
xxxxx   analysis-ik  8.12.2
```

### 6.4 测试 IK 分词器

```bash
# 测试 ik_max_word（细粒度分词）
curl -X POST "http://localhost:9200/_analyze?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "analyzer": "ik_max_word",
    "text": "中国专利技术创新系统"
  }'

# 测试 ik_smart（智能分词）
curl -X POST "http://localhost:9200/_analyze?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "analyzer": "ik_smart",
    "text": "中国专利技术创新系统"
  }'
```

---

## 七、启动 Kibana 8.12.2

```bash
docker run -d \
  --name kibana \
  -e ELASTICSEARCH_HOSTS=http://es:9200 \
  -e I18N_LOCALE=zh-CN \
  --network=heima \
  -p 5601:5601 \
  kibana:8.12.2
```

**参数说明：**

| 参数                                   | 说明                   |
| -------------------------------------- | ---------------------- |
| `ELASTICSEARCH_HOSTS=http://es:9200` | 连接 ES 地址（容器名） |
| `I18N_LOCALE=zh-CN`                  | 中文界面               |
| `--network=heima`                    | 与 ES 同一网络         |

---

## 八、验证 Kibana

```bash
# 等待 1-2 分钟后查看日志
docker logs kibana

# 测试状态
curl http://localhost:5601/api/status
```

**浏览器访问：** `http://虚拟机IP:5601`

---

## 九、常用命令

### 9.1 容器管理

```bash
# 启动
docker start es kibana

# 停止
docker stop es kibana

# 重启
docker restart es kibana

# 查看状态
docker ps | grep -E "es|kibana"

# 查看日志
docker logs -f es
docker logs -f kibana
```

### 9.2 ES 常用 API

```bash
# 集群健康状态
curl http://localhost:9200/_cluster/health?pretty

# 查看所有索引
curl http://localhost:9200/_cat/indices?v

# 查看节点信息
curl http://localhost:9200/_cat/nodes?v

# 查看插件列表
curl http://localhost:9200/_cat/plugins?v
```

---

## 十、常见问题排查

### 10.1 ES 启动失败

**问题：权限不足**

```
java.nio.file.AccessDeniedException: /usr/share/elasticsearch/data/nodes
```

**解决：**

```bash
sudo chown -R 1000:1000 /var/lib/docker/volumes/es-data/_data
sudo chown -R 1000:1000 /var/lib/docker/volumes/es-plugins/_data
```

### 10.2 IK 插件未加载

**问题：插件未识别**

**检查：**

```bash
# 确认目录结构正确
ls /var/lib/docker/volumes/es-plugins/_data/ik/

# 确认 plugin-descriptor.properties 存在
cat /var/lib/docker/volumes/es-plugins/_data/ik/plugin-descriptor.properties
```

**解决：** 确保解压后文件直接在 `ik/` 目录下，而不是 `ik/elasticsearch-analysis-ik-8.12.2/` 这样的嵌套目录。

### 10.3 Kibana 连接 ES 失败

**问题：** `Unable to retrieve version information from Elasticsearch`

**检查：**

```bash
# 确认 ES 已启动
curl http://localhost:9200

# 确认网络
docker network inspect heima
```

**解决：** 确保 ES 和 Kibana 在同一个 `heima` 网络中。

### 10.4 内存不足

**问题：** ES 启动后自动退出

**解决：** 降低 JVM 内存配置

```bash
-e "ES_JAVA_OPTS=-Xms256m -Xmx256m"
```

---

## 十一、版本对应关系

| Elasticsearch | IK 分词器 | Kibana |
| ------------- | --------- | ------ |
| 8.12.2        | 8.12.2    | 8.12.2 |
| 8.12.1        | 8.12.1    | 8.12.1 |
| 8.12.0        | 8.12.0    | 8.12.0 |
| 7.12.1        | 7.12.1    | 7.12.1 |

> ⚠️ **重要**：IK 分词器版本必须与 Elasticsearch 版本完全一致！

---

## 十二、IK 分词器下载地址汇总

| 版本   | GitHub                                                                                                        | 镜像站                                                                                      |
| ------ | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| 8.12.2 | [下载](https://github.com/infinilabs/analysis-ik/releases/download/v8.12.2/elasticsearch-analysis-ik-8.12.2.zip) | [下载](https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.12.2.zip) |
| 8.12.1 | [下载](https://github.com/infinilabs/analysis-ik/releases/download/v8.12.1/elasticsearch-analysis-ik-8.12.1.zip) | [下载](https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.12.1.zip) |
| 8.12.0 | [下载](https://github.com/infinilabs/analysis-ik/releases/download/v8.12.0/elasticsearch-analysis-ik-8.12.0.zip) | [下载](https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.12.0.zip) |

**IK 分词器 GitHub 仓库：** https://github.com/infinilabs/analysis-ik/releases

```
POST /_analyze
{
  "analyzer": "ik_smart",
  "text": "传智播客开设大学,真的泰裤辣啊！"
}

# 创建索引库和映射
PUT /heima
{
  "mappings": {
    "properties": {
      "info":{
        "type": "text",
        "analyzer": "ik_smart"
      },
      "email":{
        "type": "keyword",
        "index": "false"
      },
      "name":{
        "properties": {
          "firstName": {
            "type": "keyword"
          },
          "lastName": {
            "type": "keyword"
          }
        }
      }
    }
  }
}

# 查询索引库
GET /heima

# 删除索引库
DELETE /heima

# 修改索引库
PUT /heima/_mapping
{
  "properties": {
    "age":{
      "type": "integer"
    }
  }
}

# 新增文档
POST /heima/_doc/1
{
  "info": "黑马程序员Java讲师",
  "email": "zy@itcast.cn",
  "name": {
    "firstName": "云",
    "lastName": "赵"
  }
}

# 查询文档
GET /heima/_doc/1
GET /heima/_doc/2

# 删除文档
DELETE /heima/_doc/1

# 修改文档
# 全量修改（先删除文档在新增文档）
PUT /heima/_doc/1
{
    "info": "黑马程序员高级Java讲师",
    "email": "ZY@itcast.cn",
    "name": {
        "firstName": "云",
        "lastName": "赵"
    }
}

# 全量修改，文档2不存在，会创建文档2
PUT /heima/_doc/2
{
    "info": "黑马程序员高级Java讲师",
    "email": "ZY@itcast.cn",
    "name": {
        "firstName": "云",
        "lastName": "赵"
    }
}

# 增量修改（局部修改）
POST /heima/_update/1
{
  "doc": {
    "email": "ZhaoYun@itcast.cn"
  }
}

# 批量新增
POST /_bulk
{"index": {"_index":"heima", "_id": "3"}}
{"info": "黑马程序员C++讲师", "email": "ww@itcast.cn", "name":{"firstName": "五", "lastName":"王"}}
{"index": {"_index":"heima", "_id": "4"}}
{"info": "黑马程序员前端讲师", "email": "zhangsan@itcast.cn", "name":{"firstName": "三", "lastName":"张"}}

# 批量删除
POST /_bulk
{"delete":{"_index":"heima", "_id": "3"}}
{"delete":{"_index":"heima", "_id": "4"}}

# 批量修改
POST /_bulk
{ "update" : {"_index" : "heima" , "_id" : "1"} }
{ "doc" : {"email" : "ZY@itcast.cn"} }
{ "update" : {"_index" : "heima" , "_id" : "2"} }
{ "doc" : {"email" : "ZhaoYun@itcast.cn"} }


# 创建商品items索引库
PUT /items
{
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword"
      },
      "name":{
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "price":{
        "type": "integer"
      },
      "stock":{
        "type": "integer"
      },
      "image":{
        "type": "keyword",
        "index": false
      },
      "category":{
        "type": "keyword"
      },
      "brand":{
        "type": "keyword"
      },
      "sold":{
        "type": "integer"
      },
      "commentCount":{
        "type": "integer",
        "index": false
      },
      "isAD":{
        "type": "boolean"
      },
      "updateTime":{
        "type": "date"
      }
    }
  }
}

# 查询items索引库
GET /items

# 删除items索引库
DELETE /items

# 查询items文档
GET /items/_doc/317578

#查询items文档数量
GET /items/_count

# DSL查询快速入门，
GET /items/_search
{
  "query": {
    "match_all": {
  
    }
  }
}

# 叶子查询
# 全文检索查询
GET /items/_search
{
  "query": {
    "match": {
      "name": "脱脂牛奶"
    }
  }
}

GET /items/_search
{
  "query": {
    "multi_match": {
      "query": "脱脂牛奶",
      "fields": ["name","category"]
    }
  }
}

# 精确查询(keyword,不分词)
GET /items/_search
{
  "query": {
    "term": {
      "brand": {
        "value": "华为"
      }
    }
  }
}

GET /items/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 100000,
        "lte": 200000
      }
    }
  }
}

# 复合查询
GET /items/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {"name": "手机"}}
      ],
      "should": [
        {"term": {"brand": { "value": "vivo" }}},
        {"term": {"brand": { "value": "小米" }}}
      ],
      "must_not": [
        {"range": {"price": {"gte": 90000}}}
      ],
      "filter": [
        {"range": {"price": {"lte": 90000}}}
      ]
    }
  }
}

GET /items/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {"name": "手机"}}
      ],
      "filter": [
        {"term": {"brand": { "value": "华为" }}},
        {"range": {"price": {"gte": 90000, "lt": 159900}}}
      ]
    }
  }
}

# 排序查询
GET /items/_search
{
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "price": {
        "order": "asc"
      }
    },
    {
      "sold": "desc"
    }
  ]
}

# 基础分页
GET /items/_search
{
  "query": {
    "match_all": {}
  },
  "from": 0, 
  "size": 10, 
  "sort": [
    {
      "sold": "desc"
    },
    {
      "price": "asc"
    }
  ]
}

# 深度分页


# 高亮显示
GET /items/_search
{
  "query": {
    "match": {
      "name": "脱脂牛奶"
    }
  },
  "highlight": {
    "fields": {
      "name": {
        "pre_tags": "<em>",
        "post_tags": "</em>"
      }
    }
  }
}

# Bucket聚合
# select category count(*) from items group by category
GET /items/_search
{
  "size": 0, 
  "aggs": {
    "category_agg": {
      "terms": {
        "field": "category",
        "size": 20
      }
    },
    "brand_agg": {
      "terms": {
        "field": "brand",
        "size": 20
      }
    }
  }
}

# 条件聚合
GET /items/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "category": "手机"
          }
        },
        {
          "range": {
            "price": {
              "gte": 300000,
              "lte": 2000000
            }
          }
        }
      ]
    }
  }, 
  "size": 0, 
  "aggs": {
    "category_agg": {
      "terms": {
        "field": "brand",
        "size": 20
      }
    }
  }
}

# Metric聚合
GET /items/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "category": "手机"
          }
        }
      ]
    }
  },
  "size": 0,
  "aggs": {
    "brand_agg": {
      "terms": {
        "field": "brand",
        "size": 10
      },
      "aggs": {
        "price_stats": {
          "stats": {
            "field": "price"
          }
        }
      }
    }
  }
}

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

GET /patent_index/_search
{
  "query": {
    "match_all": {}
  }
}

```
