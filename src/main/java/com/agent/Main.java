package com.agent;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Code Explain Agent 启动 ===");

        Config.load();

        if (Config.get("api.key", "").isBlank()) {
            System.out.println("API Key 未配置！请在 src/main/resources/config.properties 中填入 api.key");
            return;
        }

        LLMClient llm = new LLMClient();

        try {
            String reply = llm.testConnection();
            System.out.println("\n========== LLM 回复 ==========");
            System.out.println(reply);
            System.out.println("===============================\n");
            System.out.println(" 阶段三第1步验证通过！");
        } catch (Exception e) {
            System.err.println("调用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}