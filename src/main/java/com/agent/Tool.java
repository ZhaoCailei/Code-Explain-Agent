package com.agent;

public interface Tool {
    String name();
    String description();
    String execute(String argsJson); // 简化：参数用 JSON 字符串，返回结果字符串
}