package com.itsm.module.ticket.controller;

import com.itsm.common.api.PageResult;
import com.itsm.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工单接口骨架:创建/受理/处理/转派/挂起/解决/关闭/重开等动作
 * 见设计文档 6.3 状态机与 9.2 API 清单。
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    /** TODO: 接入 TicketService(落库 + Flowable 启流 + SLA 时钟启动) */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.ok(Map.of("ticketNo", "IR-20260104-000001", "status", "PENDING"));
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list() {
        return Result.ok(PageResult.empty(1, 20));
    }

    /** 处理时间线(TODO: ticket_action 查询) */
    @GetMapping("/{id}/activities")
    public Result<List<Map<String, Object>>> activities() {
        return Result.ok(List.of());
    }
}

