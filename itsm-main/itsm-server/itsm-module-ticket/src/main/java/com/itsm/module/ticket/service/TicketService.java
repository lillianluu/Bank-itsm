package com.itsm.module.ticket.service;

import com.itsm.module.ticket.dto.TicketCreateCmd;

/**
 * 工单领域服务(骨架)。
 * 实现需串联:单号生成 → 落库 → Flowable 启流(businessKey=单号)→ SLA 时钟启动 →
 * 事件发布(itsm.ticket.created → 通知/分流),见设计文档 6.1/6.2。
 */
public interface TicketService {

    /** 创建工单,返回工单号 */
    String create(TicketCreateCmd cmd);
}

