package com.itsm.domain.enums;

import lombok.Getter;

/** 优先级:由 紧急度 × 影响面 矩阵自动计算(设计文档 7.3) */
@Getter
public enum PriorityLevel {

    P1("紧急"),
    P2("高"),
    P3("中"),
    P4("低");

    private final String desc;

    PriorityLevel(String desc) {
        this.desc = desc;
    }
}

