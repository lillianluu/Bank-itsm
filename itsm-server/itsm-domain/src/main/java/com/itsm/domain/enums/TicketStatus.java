package com.itsm.domain.enums;

import lombok.Getter;

/** 工单状态机(设计文档 6.3) */
@Getter
public enum TicketStatus {

    DRAFT("草稿"),
    PENDING("待受理"),
    ACCEPTED("已受理"),
    TRANSFERRED("已转派"),
    PROCESSING("处理中"),
    WAITING_USER("等待用户"),
    WAITING_VENDOR("等待供应商"),
    SUSPENDED("挂起"),
    RESOLVED("已解决"),
    CLOSED("已关闭");

    private final String desc;

    TicketStatus(String desc) {
        this.desc = desc;
    }
}

