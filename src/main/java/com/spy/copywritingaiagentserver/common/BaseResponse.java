package com.spy.server.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {

    private T data;

    private int code;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.data = data;
        this.code = code;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data,null);
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
