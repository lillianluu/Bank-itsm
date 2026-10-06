package com.itsm.engine.form;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务端表单校验:按 Schema 校验 form_data。
 * TODO: 类型/长度/枚举/联动规则校验;流程节点字段权限(隐藏/只读/可编辑)裁剪(设计文档 7.5)。
 */
@Component
public class FormValidator {

    public List<String> validate(FormSchema schema, Map<String, Object> formData) {
        List<String> errors = new ArrayList<>();
        if (schema == null || schema.getControls() == null) {
            return errors;
        }
        for (FormControl c : schema.getControls()) {
            Object value = formData == null ? null : formData.get(c.getKey());
            if (c.isRequired() && (value == null || String.valueOf(value).isBlank())) {
                errors.add(c.getLabel() + " 不能为空");
            }
        }
        return errors;
    }
}

