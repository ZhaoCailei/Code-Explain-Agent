package com.agent;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Code Explain Agent 启动 ===");
        Config.load();

        if (Config.get("api.key", "").isBlank()) {
            System.out.println(" 未配置 api.key");
            return;
        }

        LLMClient llm = new LLMClient();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ReadFileTool());

        Agent agent = new Agent(llm, registry);

        try {
            // 测试1：纯对话
            agent.run("用一句话解释 Java Stream 的 map 操作");

            // 测试2：触发工具调用（需要你本地有这个文件，或改路径）
            agent.run("帮我读取并解释 D:/Lei Homework/Code-Explain-Agent/src/main/java/com/agent/Main.java");

        } catch (Exception e) {
            System.err.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}