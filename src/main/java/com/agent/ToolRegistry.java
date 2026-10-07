package com.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public Tool get(String name) { return tools.get(name); }
    public boolean has(String name) { return tools.containsKey(name); }

    /**
     * 返回 OpenAI 兼容的 tools JSON 数组
     */
    public JsonNode toJsonSchema() {
        ArrayNode arr = mapper.createArrayNode();
        for (Tool t : tools.values()) {
            ObjectNode toolNode = mapper.createObjectNode();
            toolNode.put("type", "function");
            ObjectNode func = toolNode.putObject("function");
            func.put("name", t.name());
            func.put("description", t.description());
            func.putObject("parameters").put("type", "object");
            arr.add(toolNode);
        }
        return arr;
    }

    public List<Tool> list() {
        return new ArrayList<>(tools.values());
    }
}