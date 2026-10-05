package com.itsm.common.api;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.MDC;

/**
 * 统一响应结构:{"code":0,"message":"ok","data":{},"traceId":"..."}
 * 错误码规范见设计文档 9.1。
 */
@Getter
@ToString
public final class Result<T> {

    private final int code;
    private final String message;
    private final T data;
    private final String traceId;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isOk() {
        return code == 0;
    }
}

