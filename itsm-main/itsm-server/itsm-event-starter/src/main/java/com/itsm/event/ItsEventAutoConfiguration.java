package com.itsm.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsm.common.spi.event.EventBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 事件总线自动装配:未引入 MQ 依赖时,MQ 分支整体不可达;
 * 默认兜底进程内事件总线,保证零外部依赖可运行(设计文档 10.2.2)。
 */
@AutoConfiguration
@EnableConfigurationProperties(ItsEventProperties.class)
public class ItsEventAutoConfiguration {

    /** Kafka 可选实现:classpath 存在 spring-kafka 且 itsm.event.bus=kafka 时装配 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    @ConditionalOnProperty(name = "itsm.event.bus", havingValue = "kafka")
    static class KafkaBusConfiguration {

        @Bean
        public EventBus kafkaEventBus(ObjectProvider<KafkaTemplate<String, String>> templateProvider,
                                      ObjectMapper objectMapper) {
            KafkaTemplate<String, String> template = templateProvider.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException("itsm.event.bus=kafka 需要提供 KafkaTemplate<String,String> Bean");
            }
            return new KafkaEventBus(template, objectMapper);
        }
    }

    /** RocketMQ 可选实现:itsm.event.bus=rocketmq 且 classpath 存在 RocketMQTemplate 时装配 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
    @ConditionalOnProperty(name = "itsm.event.bus", havingValue = "rocketmq")
    static class RocketMqBusConfiguration {

        @Bean
        public EventBus rocketMqEventBus() {
            return new RocketMqEventBus();
        }
    }

    /** 默认兜底:进程内事件总线,零外部依赖 */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus inProcessEventBus(ApplicationEventPublisher publisher) {
        return new InProcessEventBus(publisher);
    }
}

