package com.itsm.domain.base;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 实体审计基类(纯 POJO):与设计文档 8.3 的审计字段约定一致。
 */
@Data
public class BaseDO {

    private Long id;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;
}

