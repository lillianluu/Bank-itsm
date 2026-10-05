package com.itsm.event;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事件总线配置(itsm.event.*):bus = inprocess(默认) | rocketmq | kafka,见设计文档 10.2.3。
 */
@Data
@ConfigurationProperties(prefix = "itsm.event")
public class ItsEventProperties {

    /** inprocess(默认) | rocketmq | kafka */
    private String bus = "inprocess";

    private Outbox outbox = new Outbox();

    @Data
    public static class Outbox {
        /** Outbox 补偿:重试间隔(秒)与最大重试次数(设计文档 10.2.4) */
        private long retryIntervalSeconds = 30;
        private int maxRetry = 5;
    }
}

