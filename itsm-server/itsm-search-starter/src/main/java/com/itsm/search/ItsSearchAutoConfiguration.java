package com.itsm.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.itsm.common.spi.search.SearchEngine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 检索自动装配:未引入 ES 依赖时,ES 实现分支整体不可达;
 * 默认兜底 PG FTS 实现,保证零外部依赖可运行(设计文档 10.2.2)。
 */
@AutoConfiguration
@EnableConfigurationProperties(ItsSearchProperties.class)
public class ItsSearchAutoConfiguration {

    /** ES 可选实现:classpath 存在 elasticsearch-java 且 itsm.search.engine=es 时装配 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ElasticsearchClient.class)
    @ConditionalOnProperty(name = "itsm.search.engine", havingValue = "es")
    static class EsSearchEngineConfiguration {

        @Bean
        public SearchEngine esSearchEngine(ObjectProvider<ElasticsearchClient> clientProvider) {
            // TODO: 未提供 ElasticsearchClient Bean 时,根据 itsm.search.es.hosts 自动构建 RestClient
            return new EsSearchEngine(clientProvider.getIfAvailable());
        }
    }

    /** 默认兜底:PG 全文检索,零外部依赖 */
    @Bean
    @ConditionalOnMissingBean(SearchEngine.class)
    public SearchEngine pgFtsSearchEngine(NamedParameterJdbcTemplate jdbcTemplate) {
        return new PgFtsSearchEngine(jdbcTemplate);
    }
}

