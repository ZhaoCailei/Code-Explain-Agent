package com.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Code Explain Agent 启动 ===");

        // 加载配置
        Config.load();
        String model = Config.get("model.name", "unknown");
        log.info("使用模型: {}", model);

        if (Config.get("api.key").isBlank()) {
            log.error("API Key 未配置！请在 src/main/resources/config.properties 中填入 api.key");
            return;
        }

        // 初始化 LLM 客户端
        LLMClient llm = new LLMClient();

        // 测试连通性
        try {
            log.info("正在测试 LLM 连通性...");
            String reply = llm.testConnection();
            System.out.println("\n========== LLM 回复 ==========");
            System.out.println(reply);
            System.out.println("===============================\n");
            log.info("阶段三第1步验证通过：LLM 接口连通成功！");
        } catch (Exception e) {
            log.error("LLM 连通测试失败: {}", e.getMessage(), e);
            System.out.println("请检查：1) API Key 是否正确  2) 网络连接是否正常  3) api.url 是否可达");
        }
    }
}