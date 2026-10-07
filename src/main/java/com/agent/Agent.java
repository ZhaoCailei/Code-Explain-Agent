package com.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Agent {
    private final LLMClient llm;
    private final ToolRegistry registry;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Message> history = new ArrayList<>();

    public Agent(LLMClient llm, ToolRegistry registry) {
        this.llm = llm;
        this.registry = registry;
    }

    public void run(String userInput) throws Exception {
        System.out.println("\n🧑 用户: " + userInput);
        history.add(new Message("user", userInput));

        // System Prompt（强引导工具调用格式）
        String systemPrompt = "你是一个代码解释助手。\n" +
                "如果需读代码，回复严格单行 JSON：{\"tool\":\"read_file\",\"args\":{\"path\":\"路径\"}}\n" +
                "如果直接回答，正常文本回复（不要带 JSON）。";

        // 合并历史（简化：只带最后一条用户消息 + 系统提示）
        String prompt = systemPrompt + "\n\n用户: " + userInput;

        String reply = llm.chat(prompt);
        System.out.println("🤖 LLM 原始回复: " + reply);

        // 解析是否工具调用
        String result = parseAndExecute(reply);
        if (result != null) {
            System.out.println("🔧 工具执行结果: " + result);
            // 回传工具结果给 LLM 生成最终回答
            String finalReply = llm.chat(systemPrompt + "\n\n用户: " + userInput
                    + "\n\n[工具结果]: " + result + "\n\n请基于结果直接回答用户：");
            System.out.println("🤖 最终回答: " + finalReply);
            history.add(new Message("assistant", finalReply));
        } else {
            System.out.println("🤖 直接回答: " + reply);
            history.add(new Message("assistant", reply));
        }
    }

    /**
     * 解析 JSON 工具调用，执行后返回结果（非工具调用返回 null）
     */
    private String parseAndExecute(String text) {
        try {
            String trim = text.trim();
            if (!trim.startsWith("{") || !trim.endsWith("}")) return null;
            JsonNode node = mapper.readTree(trim);
            if (!node.has("tool")) return null;

            String toolName = node.get("tool").asText();
            String argsJson = node.has("args") ? node.get("args").toString() : "{}";

            if (!registry.has(toolName)) {
                return "未知工具: " + toolName;
            }
            Tool tool = registry.get(toolName);
            return tool.execute(argsJson);
        } catch (Exception e) {
            return null; // 解析失败视为普通文本
        }
    }
}