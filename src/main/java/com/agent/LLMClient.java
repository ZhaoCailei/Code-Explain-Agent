package com.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端：通过 OpenAI 兼容接口调用大模型
 */
public class LLMClient {
    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String apiUrl;
    private final String modelName;

    public LLMClient() {
        this.apiKey = Config.get("api.key");
        this.apiUrl = Config.get("api.url", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        this.modelName = Config.get("model.name", "qwen2.5-coder-7b-instruct");
        log.info("LLMClient 初始化完成，模型: {}, API地址: {}", modelName, apiUrl);
    }

    /**
     * 发送单轮对话请求（非流式）
     * @param userMessage 用户输入
     * @return 模型回复文本
     * @throws IOException 网络或解析异常
     */
    public String chat(String userMessage) throws IOException {
        return chat(userMessage, new ArrayList<>());
    }

    /**
     * 发送多轮对话请求
     * @param userMessage 当前用户输入
     * @param history 历史消息（格式：List of Map，每条含 role + content）
     * @return 模型回复文本
     * @throws IOException 网络或解析异常
     */
    public String chat(String userMessage, List<Map<String, String>> history) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key 未配置，请在 config.properties 中设置 api.key");
        }

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        // 加入系统提示
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一位资深Java工程师，擅长代码解释、代码审查和设计模式分析。请用清晰的结构化方式回答用户问题。");
        messages.add(systemMsg);
        // 加入历史对话
        if (history != null) {
            messages.addAll(history);
        }
        // 加入当前输入
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2048);

        String jsonBody = mapper.writeValueAsString(requestBody);
        log.debug("发送请求体: {}", jsonBody);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown";
                log.error("LLM API 调用失败: HTTP {} - {}", response.code(), errorBody);
                throw new IOException("LLM API error " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("收到响应: {}", responseBody);

            // 解析 OpenAI 兼容格式响应
            JsonNode root = mapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("LLM 回复成功，长度: {}", content.length());
                return content;
            } else {
                throw new IOException("LLM 响应格式异常: " + responseBody);
            }
        }
    }

    /**
     * 测试连通性：发送一句简单问候
     */
    public String testConnection() throws IOException {
        log.info("测试 LLM 连通性...");
        return chat("你好，请用一句话回复确认你已就绪。");
    }
}