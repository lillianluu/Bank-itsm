package com.itsm.domain.enums;

import lombok.Getter;

/** 紧急度 */
@Getter
public enum UrgencyLevel {

    HIGH("高"),
    MEDIUM("中"),
    LOW("低");

    private final String desc;

    UrgencyLevel(String desc) {
        this.desc = desc;
    }
}

