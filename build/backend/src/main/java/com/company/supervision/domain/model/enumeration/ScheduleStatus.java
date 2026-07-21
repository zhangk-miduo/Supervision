package com.company.supervision.domain.model.enumeration;

public enum ScheduleStatus {
    ENABLED(1),
    DISABLED(0);

    private final int code;

    ScheduleStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ScheduleStatus of(Integer code) {
        if (code == null) return ENABLED;
        for (ScheduleStatus s : values()) {
            if (s.code == code) return s;
        }
        return ENABLED;
    }
}
