package com.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final Properties props = new Properties();
    private static boolean loaded = false;

    // 私有构造，防止实例化
    private Config() {}

    // 静态加载配置（供 Main 调用）
    public static void load() {
        if (loaded) return;
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                log.error("未找到 config.properties（请确认在 src/main/resources 下且未被 .gitignore 误拦）");
                throw new RuntimeException("config.properties not found");
            }
            props.load(in);
            loaded = true;
            log.info("配置加载成功，模型: {}", props.getProperty("model.name", "未知"));
        } catch (IOException e) {
            log.error("配置加载失败: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // 读取配置（带默认值）
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    /**
     * 读取整数配置，带默认值
     */
    public static int getInt(String key, int defaultValue) {
        load();
        String val = props.getProperty(key);
        if (val != null && !val.isBlank()) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // 读取配置（无默认值）
    public static String get(String key) {
        return props.getProperty(key, "");
    }
}