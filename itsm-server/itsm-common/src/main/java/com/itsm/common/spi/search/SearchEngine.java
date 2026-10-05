package com.itsm.common.spi.search;

import com.itsm.common.api.PageResult;

/**
 * 检索引擎 SPI(设计文档 10.2):
 * 默认实现 PgFtsSearchEngine(PostgreSQL FTS,零外部依赖),
 * 可选 EsSearchEngine(ES 8,ik 分词/高亮/聚合),itsm.search.engine 开关切换。
 */
public interface SearchEngine {

    String name();

    /** 写入/更新索引文档 */
    void index(String index, String id, SearchDocument doc);

    /** 删除索引文档 */
    void delete(String index, String id);

    /** 关键词检索(跨 index 可选) */
    PageResult<SearchHit> search(SearchQuery query);
}

