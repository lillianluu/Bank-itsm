package com.itsm.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.itsm.common.api.PageResult;
import com.itsm.common.spi.search.SearchDocument;
import com.itsm.common.spi.search.SearchEngine;
import com.itsm.common.spi.search.SearchHit;
import com.itsm.common.spi.search.SearchQuery;
import lombok.RequiredArgsConstructor;

/**
 * ES 8 增强检索实现(骨架):ik 中文分词、高亮、聚合排序 TODO(设计文档 10.2.4)。
 * 仅当 classpath 存在 elasticsearch-java 且 itsm.search.engine=es 时装配。
 */
@RequiredArgsConstructor
public class EsSearchEngine implements SearchEngine {

    private final ElasticsearchClient client;

    @Override
    public String name() {
        return "es";
    }

    @Override
    public void index(String index, String id, SearchDocument doc) {
        throw new UnsupportedOperationException("ES 增强检索将在 V1.1 接入,当前请使用默认 PG 实现");
    }

    @Override
    public void delete(String index, String id) {
        throw new UnsupportedOperationException("ES 增强检索将在 V1.1 接入,当前请使用默认 PG 实现");
    }

    @Override
    public PageResult<SearchHit> search(SearchQuery query) {
        throw new UnsupportedOperationException("ES 增强检索将在 V1.1 接入,当前请使用默认 PG 实现");
    }
}

