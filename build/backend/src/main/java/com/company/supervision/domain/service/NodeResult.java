package com.company.supervision.domain.service;

/**
 * 单个节点执行结果。
 */
public class NodeResult {

    private final boolean success;
    private final Object data;
    private final String message;

    private NodeResult(boolean success, Object data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static NodeResult ok(Object data, String message) {
        return new NodeResult(true, data, message);
    }

    public static NodeResult ok(String message) {
        return new NodeResult(true, null, message);
    }

    public static NodeResult fail(String message) {
        return new NodeResult(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
