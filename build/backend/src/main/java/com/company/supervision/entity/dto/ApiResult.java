package com.company.supervision.entity.dto;

public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public ApiResult() {
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 200;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(String message) {
        ApiResult<T> r = new ApiResult<>();
        r.code = 500;
        r.message = message;
        return r;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
