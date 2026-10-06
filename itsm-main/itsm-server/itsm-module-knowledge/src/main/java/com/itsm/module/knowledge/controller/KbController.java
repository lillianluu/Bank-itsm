package com.itsm.module.knowledge.controller;

import com.itsm.common.api.PageResult;
import com.itsm.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 知识库接口骨架:空间/文章/审核发布,检索走统一 SearchEngine SPI,见设计文档 7.7。
 */
@RestController
@RequestMapping("/api/v1/kb")
public class KbController {

    @GetMapping("/articles")
    public Result<PageResult<Map<String, Object>>> articles() {
        return Result.ok(PageResult.empty(1, 20));
    }
}

