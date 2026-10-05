package com.itsm.common.spi.event;

import com.itsm.common.event.DomainEvent;
import com.itsm.common.event.DomainEventListener;

/**
 * 事件总线 SPI(设计文档 10.2):
 * 默认实现 InProcessEventBus(零外部依赖),可选 RocketMQ / Kafka,
 * 通过 itsm.event.bus 开关切换,业务代码只依赖本接口。
 */
public interface EventBus {

    String name();

    void publish(DomainEvent event);

    void subscribe(String topic, DomainEventListener listener);
}

