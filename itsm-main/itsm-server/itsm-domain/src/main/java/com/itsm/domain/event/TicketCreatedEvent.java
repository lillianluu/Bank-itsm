package com.itsm.domain.event;

import com.itsm.common.event.DomainEvent;
import com.itsm.domain.enums.PriorityLevel;
import com.itsm.domain.enums.TicketType;
import lombok.Getter;

/**
 * 工单创建事件:用于驱动通知、SLA 时钟启动等(设计文档 6.1 步骤 2-5)。
 */
@Getter
public class TicketCreatedEvent extends DomainEvent {

    private final String ticketNo;
    private final TicketType ticketType;
    private final String title;
    private final PriorityLevel priority;

    public TicketCreatedEvent(String ticketNo, TicketType ticketType, String title, PriorityLevel priority) {
        this.ticketNo = ticketNo;
        this.ticketType = ticketType;
        this.title = title;
        this.priority = priority;
    }

    @Override
    public String topic() {
        return "itsm.ticket.created";
    }
}

