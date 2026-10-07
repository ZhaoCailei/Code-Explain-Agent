package com.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LLM 客户端：OpenAI 兼容协议
 * 特性：超时控制 + Jackson DTO 序列化 + 指数退避重试
 */
public class LLMClient {

    private static final okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final int maxRetry;
    private final long baseDelayMs;

    public LLMClient() {
        // 从配置读取
        this.apiKey = Config.get("api.key", "");
        this.apiUrl = Config.get("api.url", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        this.modelName = Config.get("model.name", "qwen2.5-coder-7b-instruct");

        int connectTimeout = Config.getInt("connect.timeout.ms", 5000);
        int readTimeout = Config.getInt("read.timeout.ms", 60000);
        this.maxRetry = Config.getInt("max.retry", 3);
        this.baseDelayMs = Config.getInt("retry.base.delay.ms", 1000);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();

        this.objectMapper = new ObjectMapper();

        System.out.println("[LLMClient] 初始化完成 → 模型: " + modelName);
        System.out.println("[LLMClient] API地址: " + apiUrl);
    }

    /**
     * 单轮对话（最简调用）
     */
    public String chat(String userMessage) throws Exception {
        return chat(userMessage, new ArrayList<>());
    }

    /**
     * 多轮对话（带历史）
     */
    public String chat(String userMessage, List<Map<String, String>> history) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key 未配置！请在 config.properties 中设置 api.key");
        }

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一位资深Java工程师，擅长代码解释、代码审查和设计模式分析。请用清晰的结构化方式回答用户问题。");
        messages.add(systemMsg);

        // 历史对话
        if (history != null) {
            messages.addAll(history);
        }

        // 当前用户输入
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

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 指数退避重试
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetry; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(RequestBody.create(jsonBody, JSON))
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "unknown";
                        throw new RuntimeException("HTTP " + response.code() + ": " + errorBody);
                    }

                    String responseBody = response.body().string();

                    // 解析 OpenAI 兼容格式
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
                    com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");

                    if (choices.isArray() && choices.size() > 0) {
                        String content = choices.get(0).path("message").path("content").asText();
                        return content;
                    } else {
                        throw new RuntimeException("响应格式异常: " + responseBody);
                    }
                }

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetry - 1) {
                    long delay = baseDelayMs * (1L << attempt); // 指数退避: 1s, 2s, 4s...
                    System.err.println("[LLMClient] 第 " + (attempt + 1) + " 次调用失败，"
                            + delay + "ms 后重试。原因: " + e.getMessage());
                    Thread.sleep(delay);
                }
            }
        }

        throw new RuntimeException("LLM 调用失败（已重试 " + maxRetry + " 次）", lastException);
    }

    /**
     * 测试连通性
     */
    public String testConnection() throws Exception {
        System.out.println("[LLMClient] 测试连通性...");
        return chat("你好，请用一句话回复确认你已就绪。");
    }
}