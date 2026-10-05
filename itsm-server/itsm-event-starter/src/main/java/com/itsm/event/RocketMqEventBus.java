package com.itsm.event;

import com.itsm.common.event.DomainEvent;
import com.itsm.common.event.DomainEventListener;
import com.itsm.common.spi.event.EventBus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RocketMQ 事件总线实现(可选):itsm.event.bus=rocketmq 时装配。
 * 接入步骤(BOM 已预留版本注释):
 * 1) 引入 rocketmq-spring-boot-starter 并启用 BOM 版本;
 * 2) 注入 RocketMQTemplate 实现 publish;
 * 3) 通过 RocketMQMessageListener 容器注册订阅分发到 listeners。
 */
@Slf4j
public class RocketMqEventBus implements EventBus {

    private final Map<String, List<DomainEventListener>> listeners = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "rocketmq";
    }

    @Override
    public void publish(DomainEvent event) {
        throw new UnsupportedOperationException("RocketMQ 集成待接入:请引入 rocketmq-spring-boot-starter 并补全实现");
    }

    @Override
    public void subscribe(String topic, DomainEventListener listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }
}

