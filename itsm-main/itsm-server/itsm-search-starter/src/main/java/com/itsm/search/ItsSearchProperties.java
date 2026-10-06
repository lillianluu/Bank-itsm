package com.itsm.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 检索配置(itsm.search.*):engine = pg(默认) | es,见设计文档 10.2.3。
 */
@Data
@ConfigurationProperties(prefix = "itsm.search")
public class ItsSearchProperties {

    /** pg(默认) | es */
    private String engine = "pg";

    private Es es = new Es();

    @Data
    public static class Es {
        private List<String> hosts = List.of("http://127.0.0.1:9200");
        private String username = "";
        private String password = "";
    }
}

