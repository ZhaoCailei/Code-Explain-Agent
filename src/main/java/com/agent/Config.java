package com.agent;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                logger.info("配置文件 config.properties 加载成功");
            } else {
                logger.warn("未找到 config.properties，使用默认空配置");
            }
        } catch (Exception e) {
            logger.error("加载配置文件失败", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}