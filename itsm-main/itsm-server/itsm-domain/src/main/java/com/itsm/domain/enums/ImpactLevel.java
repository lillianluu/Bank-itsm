package com.itsm.domain.enums;

import lombok.Getter;

/** 影响面 */
@Getter
public enum ImpactLevel {

    WIDE("广"),
    PARTIAL("局部"),
    SINGLE("单点");

    private final String desc;

    ImpactLevel(String desc) {
        this.desc = desc;
    }
}

