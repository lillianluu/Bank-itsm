package com.itsm.common.spi.search;

/**
 * 检索命中结果。
 */
public record SearchHit(String index, String id, String title, double score) {
}

