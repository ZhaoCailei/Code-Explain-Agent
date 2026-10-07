package com.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * LLM 客户端：OpenAI 兼容协议
 * 特性：超时控制 + Jackson 序列化 + 指数退避重试 + 工具描述注入
 */
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

        System.out.println("[LLMClient] 初始化完成 → 模型: " + modelName);
    }

    // ========== 方法1：单轮对话（最简） ==========

    public String chat(String userMessage) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key 未配置！请在 config.properties 中设置 api.key");
        }

        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2048);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        return doRequest(jsonBody);
    }

    // ========== 方法2：带工具描述的对话 ==========

    public String chat(String userMessage, List<Tool> tools) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("API Key 未配置！");
        }

        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);

        // messages
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个代码解释助手。如需读代码请调用 read_file 工具。");
        messages.add(systemMsg);
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        requestBody.set("messages", messages);

        // tools 字段（OpenAI 兼容格式）
        ArrayNode toolsArray = objectMapper.createArrayNode();
        for (Tool t : tools) {
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("type", "function");
            ObjectNode func = toolNode.putObject("function");
            func.put("name", t.name());
            func.put("description", t.description());
            func.putObject("parameters").put("type", "object");
            toolsArray.add(toolNode);
        }
        requestBody.set("tools", toolsArray);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2048);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        return doRequest(jsonBody);
    }

    // ========== 私有方法：执行 HTTP 请求 + 重试 ==========

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
                        String errorBody = response.body() != null ? response.body().string() : "unknown";
                        throw new RuntimeException("HTTP " + response.code() + ": " + errorBody);
                    }

                    String responseBody = response.body().string();
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode choices = root.path("choices");

                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode message = choices.get(0).path("message");
                        // 优先检查是否有 tool_calls（标准 Function Call 返回）
                        JsonNode toolCalls = message.path("tool_calls");
                        if (toolCalls.isArray() && toolCalls.size() > 0) {
                            // 返回 tool_call 的 JSON 字符串，让 Agent 解析
                            return objectMapper.writeValueAsString(toolCalls);
                        }
                        // 否则返回普通文本
                        return message.path("content").asText();
                    } else {
                        throw new RuntimeException("响应格式异常: " + responseBody);
                    }
                }

            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetry - 1) {
                    long delay = baseDelayMs * (1L << attempt);
                    System.err.println("[LLMClient] 第 " + (attempt + 1) + " 次调用失败，"
                            + delay + "ms 后重试。原因: " + e.getMessage());
                    Thread.sleep(delay);
                }
            }
        }

        throw new RuntimeException("LLM 调用失败（已重试 " + maxRetry + " 次）", lastException);
    }

    // ========== 测试连通性 ==========

    public String testConnection() throws Exception {
        System.out.println("[LLMClient] 测试连通性...");
        return chat("你好，请用一句话回复确认你已就绪。");
    }
}