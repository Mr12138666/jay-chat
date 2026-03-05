package com.sunrisejay.jaychat.common.exception;

/**
 * 业务异常类
 * 用于抛出业务逻辑相关的异常
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}
