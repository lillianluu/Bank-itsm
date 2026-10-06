package com.itsm.engine.rule;

import org.springframework.stereotype.Component;

/**
 * 默认规则引擎骨架:TODO 规则表驱动(条件:标题关键词/渠道/目录 → 动作:指定组/优先级/标签),
 * 见设计文档 7.2 智能分流规则。
 */
@Component
public class DefaultRuleEngine implements RuleEngine {

    @Override
    public String dispatch(RoutingContext context) {
        return null;
    }
}

