package com.itsm.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsm.common.event.DomainEvent;
import com.itsm.common.event.DomainEventListener;
import com.itsm.common.spi.event.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kafka 事件总线实现(可选):itsm.event.bus=kafka 且 classpath 存在 spring-kafka 时装配。
 * TODO: 订阅侧注册 KafkaMessageListenerContainer 并反序列化分发到 listeners。
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaEventBus implements EventBus {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, List<DomainEventListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "kafka";
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            kafkaTemplate.send(event.topic(), event.getEventId(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new IllegalStateException("Kafka 事件发布失败: " + event.topic(), e);
        }
    }

    @Override
    public void subscribe(String topic, DomainEventListener listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
        // TODO: 启动监听容器并分发
    }
}

