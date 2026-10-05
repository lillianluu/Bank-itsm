package com.itsm.common.spi.search;

/**
 * 索引文档:extraJson 为业务扩展字段的原始 JSON(存储为 JSONB)。
 */
public record SearchDocument(String title, String content, String extraJson) {
}

