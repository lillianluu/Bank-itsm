package com.itsm.common.exception;

import com.itsm.common.api.ErrorCode;
import lombok.Getter;

/**
 * 业务异常:由全局异常处理器统一转换为 Result 结构(V1.0 接入)。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND);
    }

    public static BusinessException ticketNotFound() {
        return new BusinessException(ErrorCode.TICKET_NOT_FOUND);
    }

    public static BusinessException ticketStateIllegal(String message) {
        return new BusinessException(ErrorCode.TICKET_STATE_ILLEGAL, message);
    }
}

