package com.itsm.domain.enums;

import lombok.Getter;

/** 工单渠道(设计文档 7.3) */
@Getter
public enum ChannelType {

    PORTAL("自助门户"),
    EMAIL("邮件"),
    IM("即时消息"),
    PHONE("电话"),
    MONITOR("监控告警"),
    API("OpenAPI");

    private final String desc;

    ChannelType(String desc) {
        this.desc = desc;
    }
}

