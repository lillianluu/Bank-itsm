package com.itsm.engine.form;

import lombok.Builder;
import lombok.Data;

/** 表单联动规则:目标字段在 dependsOn 满足条件时的显隐/必填/可编辑 */
@Data
@Builder
public class FormRule {

    public enum RuleType { VISIBLE_WHEN, REQUIRED_WHEN, EDITABLE_WHEN }

    private RuleType type;
    private String field;
    private String dependsOn;
    /** eq | ne | in */
    private String operator;
    private String value;
}

