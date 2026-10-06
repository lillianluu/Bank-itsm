package com.itsm.module.report.controller;

import com.itsm.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 服务台看板接口骨架(设计文档 7.8) */
@RestController
@RequestMapping("/api/v1/dashboard")
public class ReportController {

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(Map.of(
                "todayNew", 0,
                "pending", 0,
                "processing", 0,
                "overdue", 0));
    }
}

