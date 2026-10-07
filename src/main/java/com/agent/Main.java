package com.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("【阶段二】Agent 启动中...");

        // 验证配置读取
        String model = Config.get("model", "test-model-stage2");
        logger.info("当前加载的模型配置: {}", model);

        String apiKey = Config.get("api.key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("API Key 未配置（符合预期占位状态）");
        }

        logger.info("【阶段二】骨架与日志/配置层验证通过");
    }
}