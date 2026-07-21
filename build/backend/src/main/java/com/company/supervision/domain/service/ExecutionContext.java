package com.company.supervision.domain.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点间传递的临时执行上下文。一次执行对应一个 ExecutionContext。
 */
public class ExecutionContext {

    private final Long executionId;
    private final Map<String, Object> data = new HashMap<>();

    public ExecutionContext(Long executionId) {
        this.executionId = executionId;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Map<String, Object> getData() {
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object v = data.get(key);
        return v == null ? null : type.cast(v);
    }
}
