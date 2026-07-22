package com.company.supervision.domain.model.enumeration;

public enum ExecutionStatus {
    SUCCESS(0),
    FAILED(1),
    RUNNING(2),
    PARTIAL_SUCCESS(3);

    private final int code;

    ExecutionStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ExecutionStatus of(Integer code) {
        if (code == null) return RUNNING;
        for (ExecutionStatus s : values()) {
            if (s.code == code) return s;
        }
        return RUNNING;
    }
}
