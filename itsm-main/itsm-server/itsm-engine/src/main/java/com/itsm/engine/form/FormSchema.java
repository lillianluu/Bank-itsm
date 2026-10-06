package com.itsm.engine.form;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 表单 Schema(设计文档 7.5):控件树 + 联动规则,
 * 服务端校验与前端渲染器同源消费;数据落 ticket.form_data(JSONB)。
 */
@Data
@Builder
public class FormSchema {

    /** 控件列表(骨架为平铺;复杂布局嵌套待扩展) */
    private List<FormControl> controls;

    /** 联动/显隐/必填规则 */
    private List<FormRule> rules;
}

