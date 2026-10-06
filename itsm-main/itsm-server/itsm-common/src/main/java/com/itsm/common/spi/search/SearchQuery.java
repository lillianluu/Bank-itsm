package com.itsm.common.spi.search;

import java.util.List;

/**
 * 统一检索查询参数(分页规范见设计文档 9.1)。
 */
public record SearchQuery(String keyword, List<String> indexes, int pageNum, int pageSize) {

    public SearchQuery {
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 20;
        }
        indexes = indexes == null ? List.of() : List.copyOf(indexes);
        keyword = keyword == null ? "" : keyword;
    }

    public int offset() {
        return (pageNum - 1) * pageSize;
    }
}

