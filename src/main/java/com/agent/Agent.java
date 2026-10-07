package com.agent;

import com.agent.Tool;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class Agent {
    private LLMClient llm;
    private ToolRegistry registry;
    private List<Message> history;
    private Memory memory;

    public Agent(LLMClient llm, ToolRegistry registry) {
        this.llm = llm;
        this.registry = registry;
        this.memory = Memory.load();
        this.history = new java.util.ArrayList<>();
    }

    public void run(String userInput) throws Exception {
        System.out.println("🧑 用户: " + userInput);
        history.add(new Message("user", userInput));
        memory.add("user", userInput);

        int maxIter = 5;
        for (int i = 0; i < maxIter; i++) {
            ChatResponse resp = llm.chat(history, registry);
            Message assistantMsg = resp.getChoices().get(0).getMessage();
            JsonNode toolCalls = resp.getFirstToolCalls();

            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                System.out.println("🔧 LLM 请求调用工具...");
                history.add(assistantMsg);
                memory.add("assistant", "[tool_calls]");

                for (JsonNode tc : toolCalls) {
                    String toolCallId = tc.path("id").asText("call_" + System.currentTimeMillis());
                    String fn = tc.path("function").path("name").asText("");
                    String args = tc.path("function").path("arguments").asText("{}");
                    System.out.println("  → 工具: " + fn + " 参数: " + args);

                    if (!registry.has(fn)) {
                        System.err.println("  ❌ 未知工具: " + fn);
                        continue;
                    }
                    Tool tool = registry.get(fn);
                    String result = tool.execute(args);
                    System.out.println("  ← 结果: " + result.substring(0, Math.min(200, result.length())));

                    memory.addToolResult(fn, result);

                    Message toolMsg = new Message("tool", result);
                    toolMsg.setTool_call_id(toolCallId);
                    toolMsg.setName(fn);
                    history.add(toolMsg);
                }
                System.out.println("  ↩ 回传结果给 LLM...");
                continue;
            } else {
                String finalAnswer = assistantMsg.getContent();
                System.out.println("🤖 回答: " + finalAnswer);
                history.add(assistantMsg);
                memory.add("assistant", finalAnswer);
                break;
            }
        }
    }
}