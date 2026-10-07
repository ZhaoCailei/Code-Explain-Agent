package com.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class Agent {

    private final LLMClient llm;
    private final ToolRegistry registry;
    private final ObjectMapper mapper;
    private final List<Message> history;

    public Agent(LLMClient llm, ToolRegistry registry) {
        this.llm = llm;
        this.registry = registry;
        this.mapper = new ObjectMapper();
        this.history = new ArrayList<>();

        // System message
        Message system = new Message("system",
                "你是一个代码解释助手。用户会给你代码文件，你需要调用 read_file 工具读取后解释。");
        history.add(system);
    }

    /**
     * 主循环：发送用户输入 → LLM 可能返回 tool_calls → 执行 → 回传 → LLM 最终回答
     */
    public void run(String userInput) throws Exception {
        System.out.println("\n🧑 用户: " + userInput);

        // 添加用户消息
        history.add(new Message("user", userInput));

        int maxIterations = 5; // 防止无限循环
        for (int i = 0; i < maxIterations; i++) {

            // 调用 LLM
            ChatResponse resp = llm.chat(history, registry);
            Message assistantMsg = resp.getChoices().get(0).getMessage();

            // 检查是否有 tool_calls
            JsonNode toolCalls = resp.getFirstToolCalls();

            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                // LLM 要求调用工具
                System.out.println("🔧 LLM 请求调用工具...");

                // 把 assistant 的 tool_calls 消息加入历史
                history.add(assistantMsg);

                // 逐个执行工具调用
                for (JsonNode tc : toolCalls) {
                    String toolCallId = tc.path("id").asText("call_" + System.currentTimeMillis());
                    String functionName = tc.path("function").path("name").asText("");
                    String arguments = tc.path("function").path("arguments").asText("{}");

                    System.out.println("  → 工具: " + functionName + " 参数: " + arguments);

                    if (!registry.has(functionName)) {
                        System.err.println("  ❌ 未知工具: " + functionName);
                        continue;
                    }

                    Tool tool = registry.get(functionName);
                    String result = tool.execute(arguments);

                    System.out.println("  ← 结果: " + result.substring(0, Math.min(200, result.length()))
                            + (result.length() > 200 ? "..." : ""));

                    // 构造 tool 角色消息回传
                    Message toolMsg = new Message("tool", result);
                    toolMsg.setTool_call_id(toolCallId);
                    toolMsg.setName(functionName);
                    history.add(toolMsg);
                }

                // 继续循环，让 LLM 基于工具结果生成回答
                System.out.println("  ↩ 回传结果给 LLM...");
                continue;

            } else {
                // 没有 tool_calls，是最终文本回答
                String finalAnswer = assistantMsg.getContent();
                System.out.println("🤖 回答: " + finalAnswer);
                history.add(assistantMsg);
                break;
            }
        }
    }

    public List<Message> getHistory() { return history; }
}