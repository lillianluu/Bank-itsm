package com.itsm.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置(itsm.storage.*):type = local(默认) | s3,见设计文档 10.2.3。
 */
@Data
@ConfigurationProperties(prefix = "itsm.storage")
public class ItsStorageProperties {

    /** local(默认) | s3 */
    private String type = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String path = "./data/files";
        private String urlPrefix = "/api/v1/files";
    }

    @Data
    public static class S3 {
        private String endpoint = "http://127.0.0.1:9000";
        private String bucket = "itsm";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyle = true;
        private String region = "us-east-1";
    }
}

