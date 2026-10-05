package com.itsm.engine.form;

import lombok.Builder;
import lombok.Data;

/** 表单控件定义 */
@Data
@Builder
public class FormControl {

    public enum ControlType {
        TEXT, TEXTAREA, NUMBER, DATE, TIME, SWITCH,
        SELECT, MULTI_SELECT, CASCADER, DICT,
        USER, DEPT, ATTACHMENT, TABLE, LABEL
    }

    private ControlType type;
    private String key;
    private String label;
    private boolean required;
    /** 选项值集(SELECT/MULTI_SELECT/CASCADER 使用) */
    private Object options;
}

