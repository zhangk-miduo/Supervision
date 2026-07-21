package com.company.supervision.domain.model.enumeration;

public enum NodeType {
    HTTP("http"),
    CONDITION("condition"),
    WECHAT("wechat");

    private final String code;

    NodeType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static NodeType of(String code) {
        if (code == null) return HTTP;
        for (NodeType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("Unknown node type: " + code);
    }
}
