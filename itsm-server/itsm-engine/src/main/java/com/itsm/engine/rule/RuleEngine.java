package com.itsm.engine.rule;

/**
 * 规则引擎接口(设计文档 10.4):智能分流规则、自动化动作(改单/派单/通知/调 API)。
 */
public interface RuleEngine {

    /** 返回目标处理组编码;null 表示无法自动分流,转人工分派 */
    String dispatch(RoutingContext context);
}

