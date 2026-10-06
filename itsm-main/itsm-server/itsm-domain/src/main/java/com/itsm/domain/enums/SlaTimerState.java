package com.itsm.domain.enums;

import lombok.Getter;

/** SLA 时钟状态(设计文档 6.4) */
@Getter
public enum SlaTimerState {

    RUNNING("计时中"),
    PAUSED("已暂停"),
    STOPPED("已停止");

    private final String desc;

    SlaTimerState(String desc) {
        this.desc = desc;
    }
}

