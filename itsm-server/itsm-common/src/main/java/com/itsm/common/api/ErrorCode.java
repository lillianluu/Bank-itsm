package com.itsm.common.api;

import lombok.Getter;

/**
 * 错误码枚举:平台段 4xxxx/5xxxx;业务域按段扩展,详见设计文档 9.1。
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),

    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或凭证失效"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),
    NOT_IMPLEMENTED(50100, "功能尚未实现"),
    INTERNAL_ERROR(50000, "系统内部错误"),

    // 工单域
    TICKET_NOT_FOUND(40401, "工单不存在"),
    TICKET_STATE_ILLEGAL(40901, "工单当前状态不允许该操作");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

