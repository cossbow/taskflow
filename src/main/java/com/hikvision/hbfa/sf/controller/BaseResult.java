package com.hikvision.hbfa.sf.controller;

import lombok.Getter;

@Getter
public class BaseResult<T> {
    private final int code;
    private final String msg;
    private final T data;

    public BaseResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    //

    private static final BaseResult<Object> EMPTY_SUCCESS = success(null);

    @SuppressWarnings("unchecked")
    public static <T> BaseResult<T> success() {
        return (BaseResult<T>) EMPTY_SUCCESS;
    }

    public static <T> BaseResult<T> success(T data) {
        return new BaseResult<>(0, null, data);
    }

    public static <T> BaseResult<T> error(int code, String msg) {
        return new BaseResult<>(code, msg, null);
    }

}
