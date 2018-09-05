package com.onesatoshi.mm;

public enum PrePageEnum {

    UNKNOWN(-1, "未知"),

    // 从附近的人页面跳转到详细资料页
    NEARBY(0, "从附近的人页面跳转到详细资料页"),

    // 从打招呼页面跳转到详细资料页
    SAYHI(1, "从打招呼页面跳转到详细资料页");

    private Integer type;

    private String name;

    PrePageEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
