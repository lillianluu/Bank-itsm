package com.itsm.search;

import com.itsm.common.api.PageResult;
import com.itsm.common.spi.search.SearchDocument;
import com.itsm.common.spi.search.SearchEngine;
import com.itsm.common.spi.search.SearchHit;
import com.itsm.common.spi.search.SearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * PG 全文检索默认实现:tsvector('simple') + GIN,零外部依赖。
 * 中文增强可插 zhparser 扩展或 pg_trgm;要求更高时切换 ES 实现
 * (itsm.search.engine=es,见设计文档 10.2.4)。
 * 依赖表 search_index 由 V1__init.sql 创建。
 */
@RequiredArgsConstructor
public class PgFtsSearchEngine implements SearchEngine {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public String name() {
        return "pg";
    }

    @Override
    public void index(String index, String id, SearchDocument doc) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("indexName", index)
                .addValue("refId", id)
                .addValue("title", doc.title())
                .addValue("content", doc.content())
                .addValue("extra", doc.extraJson());
        jdbc.update("""
                INSERT INTO search_index (index_name, ref_id, title, content, extra, tsv)
                VALUES (:indexName, :refId, :title, :content, CAST(:extra AS jsonb),
                        to_tsvector('simple', coalesce(:title, '') || ' ' || coalesce(:content, '')))
                ON CONFLICT (index_name, ref_id) DO UPDATE
                SET title = EXCLUDED.title,
                    content = EXCLUDED.content,
                    extra = EXCLUDED.extra,
                    tsv = EXCLUDED.tsv,
                    updated_at = now()
                """, p);
    }

    @Override
    public void delete(String index, String id) {
        jdbc.update("DELETE FROM search_index WHERE index_name = :indexName AND ref_id = :refId",
                Map.of("indexName", index, "refId", id));
    }

    @Override
    public PageResult<SearchHit> search(SearchQuery query) {
        String where = query.indexes().isEmpty() ? "" : " AND index_name IN (:indexes) ";
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("kw", query.keyword())
                .addValue("indexes", query.indexes())
                .addValue("limit", query.pageSize())
                .addValue("offset", query.offset());

        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM search_index, plainto_tsquery('simple', :kw) q WHERE tsv @@ q" + where,
                p, Long.class);

        List<SearchHit> hits = jdbc.query("""
                        SELECT index_name, ref_id, title, ts_rank(tsv, q) AS score
                        FROM search_index, plainto_tsquery('simple', :kw) q
                        WHERE tsv @@ q%s
                        ORDER BY score DESC
                        LIMIT :limit OFFSET :offset
                        """.formatted(where),
                p,
                (rs, i) -> new SearchHit(rs.getString("index_name"), rs.getString("ref_id"),
                        rs.getString("title"), rs.getDouble("score")));

        return PageResult.of(total == null ? 0 : total, query.pageNum(), query.pageSize(), hits);
    }
}

