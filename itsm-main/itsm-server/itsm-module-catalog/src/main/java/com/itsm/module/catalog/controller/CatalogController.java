package com.itsm.module.catalog.controller;

import com.itsm.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 服务目录接口骨架:目录树/条目/表单定义管理,见设计文档 7.4、7.5。
 */
@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    @GetMapping("/catalogs")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(List.of());
    }

    @GetMapping("/catalog-items")
    public Result<List<Map<String, Object>>> items() {
        return Result.ok(List.of());
    }
}

