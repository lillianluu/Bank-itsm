package com.itsm.module.ticket.dto;

import java.util.Map;

/** 创建工单命令(完整字段见设计文档 7.3 工单主表单) */
public record TicketCreateCmd(String type, String title, String catalogItemCode,
                              Map<String, Object> formData) {
}

