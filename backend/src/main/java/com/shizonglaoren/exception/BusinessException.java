package com.shizonglaoren.exception;

import lombok.Getter;

/**
 * 业务异常（HTTP 200，但业务逻辑错误）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 参数错误 */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    /** 未授权 */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    /** 资源不存在 */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    /** 服务器错误 */
    public static BusinessException serverError(String message) {
        return new BusinessException(500, message);
    }
}
