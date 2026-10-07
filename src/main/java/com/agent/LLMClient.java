package com.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LLMClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final int maxRetry;
    private final long baseDelayMs;

    public LLMClient() {
        this.apiKey = Config.get("api.key", "");
        this.apiUrl = Config.get("api.url",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        this.modelName = Config.get("model.name", "qwen-turbo");

        int connectTimeout = Config.getInt("connect.timeout.ms", 5000);
        int readTimeout = Config.getInt("read.timeout.ms", 60000);
        this.maxRetry = Config.getInt("max.retry", 3);
        this.baseDelayMs = Config.getInt("retry.base.delay.ms", 1000);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * 标准多轮对话（支持 tools）
     */
    public ChatResponse chat(List<Message> messages, ToolRegistry registry) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key 未配置！");
        }

        ChatRequest req = new ChatRequest();
        req.setModel(modelName);
        req.setMessages(messages);
        req.setTemperature(0.3);
        req.setMax_tokens(2048);

        if (registry != null && registry.list().size() > 0) {
            req.setTools(registry.toJsonSchema());
        }

        String jsonBody = objectMapper.writeValueAsString(req);
        String responseBody = doRequest(jsonBody);
        return objectMapper.readValue(responseBody, ChatResponse.class);
    }

    /**
     * 简易单轮（无工具）
     */
    public String chat(String prompt) throws Exception {
        List<Message> msgs = new java.util.ArrayList<>();
        msgs.add(new Message("user", prompt));
        ChatResponse resp = chat(msgs, null);
        return resp.getFirstContent();
    }

    private String doRequest(String jsonBody) throws Exception {
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
                        String err = response.body() != null ? response.body().string() : "unknown";
                        throw new RuntimeException("HTTP " + response.code() + ": " + err);
                    }
                    return response.body().string();
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetry - 1) {
                    long delay = baseDelayMs * (1L << attempt);
                    System.err.println("[LLMClient] 第 " + (attempt + 1) + " 次失败，" + delay + "ms 后重试: " + e.getMessage());
                    Thread.sleep(delay);
                }
            }
        }
        throw new RuntimeException("LLM 调用失败（已重试 " + maxRetry + " 次）", lastException);
    }

    public String testConnection() throws Exception {
        return chat("你好，用一句话确认就绪。");
    }
}