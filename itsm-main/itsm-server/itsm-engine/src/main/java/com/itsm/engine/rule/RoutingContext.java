package com.itsm.engine.rule;

import java.util.Map;

/** 分流上下文:关键词/渠道/目录条目/表单数据 */
public record RoutingContext(String keyword, String channel, String catalogItemCode, Map<String, Object> formData) {
}

