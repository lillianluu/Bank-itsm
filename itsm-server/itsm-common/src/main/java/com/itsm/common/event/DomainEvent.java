package com.itsm.common.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * 领域事件基类。topic 命名约定:"itsm.&lt;域&gt;.&lt;动作&gt;",如 itsm.ticket.created。
 * 通过 EventBus SPI 发布;默认进程内实现同步分发,可选 MQ 异步(设计文档 10.2)。
 */
public abstract class DomainEvent implements Serializable {

    private final String eventId = UUID.randomUUID().toString();
    private final Instant occurredAt = Instant.now();

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** 事件主题,如 itsm.ticket.created */
    public abstract String topic();
}

