package com.itsm.common.event;

/**
 * 事件监听器 SPI:配合 EventBus#subscribe(topic, listener) 使用。
 */
@FunctionalInterface
public interface DomainEventListener {

    void onEvent(DomainEvent event);
}

