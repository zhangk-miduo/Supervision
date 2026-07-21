package com.company.supervision.domain.model.enumeration;

public enum ConditionOperator {
    EQ("="),
    NEQ("!="),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    CONTAINS("contains"),
    EMPTY("empty");

    private final String code;

    ConditionOperator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ConditionOperator of(String code) {
        if (code == null) return EQ;
        for (ConditionOperator o : values()) {
            if (o.code.equalsIgnoreCase(code)) return o;
        }
        throw new IllegalArgumentException("Unknown operator: " + code);
    }
}
