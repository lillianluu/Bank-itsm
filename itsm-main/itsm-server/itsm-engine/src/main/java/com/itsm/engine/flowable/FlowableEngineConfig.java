package com.itsm.engine.flowable;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 引擎定制(设计文档 10.4-A):
 * 流程图中文字体设置;流程与业务同库同事务,工单通过 business_key=单号 关联流程实例。
 */
@Configuration(proxyBeanMethods = false)
public class FlowableEngineConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration configuration) {
        configuration.setActivityFontName("Microsoft YaHei");
        configuration.setAnnotationFontName("Microsoft YaHei");
        configuration.setLabelFontName("Microsoft YaHei");
    }
}

