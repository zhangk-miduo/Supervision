package com.company.supervision.domain.model.enumeration;

public enum TaskStatus {
    ENABLED(1),
    DISABLED(0);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TaskStatus of(Integer code) {
        if (code == null) return ENABLED;
        for (TaskStatus s : values()) {
            if (s.code == code) return s;
        }
        return ENABLED;
    }
}
