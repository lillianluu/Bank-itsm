package com.itsm.domain.enums;

import lombok.Getter;

/** 工单类型:事件(Incident)/ 服务请求(Service Request) */
@Getter
public enum TicketType {

    INCIDENT("事件"),
    REQUEST("服务请求");

    private final String desc;

    TicketType(String desc) {
        this.desc = desc;
    }
}

