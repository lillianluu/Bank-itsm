package com.itsm.event;

import com.itsm.common.event.DomainEvent;
import com.itsm.common.event.DomainEventListener;
import com.itsm.common.spi.event.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内事件总线(默认实现,零外部依赖):
 * 1) 编程式订阅:subscribe(topic, listener) 同步分发,单监听器异常不影响其他;
 * 2) Spring 事件:publish 同步发布,供 @EventListener / @TransactionalEventListener(AFTER_COMMIT) 使用;
 * TODO: Outbox 表 + 定时补偿保证至少一次投递(设计文档 10.2.4);启用 MQ 后复用同一 Outbox 语义。
 */
@Slf4j
public class InProcessEventBus implements EventBus {

    private final ApplicationEventPublisher publisher;
    private final Map<String, List<DomainEventListener>> listeners = new ConcurrentHashMap<>();

    public InProcessEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public String name() {
        return "inprocess";
    }

    @Override
    public void publish(DomainEvent event) {
        listeners.getOrDefault(event.topic(), List.of()).forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("事件监听处理失败, topic={}, eventId={}", event.topic(), event.getEventId(), e);
            }
        });
        publisher.publishEvent(event);
    }

    @Override
    public void subscribe(String topic, DomainEventListener listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
}

