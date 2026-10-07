package com.agent;

import com.agent.ReadFileTool;
import com.agent.RunCodeTool;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Code Explain Agent 启动（阶段四）===");
        Config.load();
        if (Config.get("api.key", "").isBlank()) {
            System.out.println(" 未配置 api.key");
            return;
        }

        LLMClient llm = new LLMClient();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ReadFileTool());
        registry.register(new RunCodeTool()); // 注册沙箱

        Agent agent = new Agent(llm, registry);

        try {
            // 测试纯对话 + 记忆
            agent.run("用一句话解释 Java 的反射机制");
            // 测试工具调用：执行代码
            agent.run("帮我写一段 Java 代码打印 1 到 3，并执行它");
        } catch (Exception e) {
            System.err.println("异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}