package com.agent;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Code Explain Agent 启动 ===");
        Config.load();

        if (Config.get("api.key", "").isBlank()) {
            System.out.println("未配置 api.key");
            return;
        }

        LLMClient llm = new LLMClient();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ReadFileTool());

        Agent agent = new Agent(llm, registry);

        try {
            // 测试1：直接回答
            agent.run("用一句话解释什么是 Java 的 Stream？");
            // 测试2：触发工具调用（模型按 Prompt 返回 JSON）
            agent.run("帮我看下 src/main/java/com/agent/Main.java 的代码大意");
        } catch (Exception e) {
            System.err.println("Agent 异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}