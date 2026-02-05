package com.patent.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.util.List;

/**
 * Elasticsearch专利文档
 * 字段命名与MySQL数据库保持一致（下划线风格）
 */
@Data
@Document(indexName = "patent_index")
public class PatentDocument {

    @Id
    private String id;

    @Field(name = "publication_no", type = FieldType.Keyword)
    private String publicationNo;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(name = "abstract_text", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String abstractText;

    @Field(type = FieldType.Keyword)
    private String applicant;

    @Field(name = "publication_date", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd")
    private LocalDate publicationDate;

    @Field(name = "domain_codes", type = FieldType.Keyword)
    private List<String> domainCodes;

    @Field(name = "domain_section", type = FieldType.Keyword)
    private String domainSection;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private List<String> entities;

    @Field(name = "entity_types", type = FieldType.Keyword)
    private List<String> entityTypes;

    @Field(name = "parse_status", type = FieldType.Keyword)
    private String parseStatus;

    @Field(name = "source_type", type = FieldType.Keyword)
    private String sourceType;

    @Field(name = "created_at", type = FieldType.Date, format = DateFormat.date)
    private LocalDate createdAt;

    @Field(name = "updated_at", type = FieldType.Date, format = DateFormat.date)
    private LocalDate updatedAt;
}
