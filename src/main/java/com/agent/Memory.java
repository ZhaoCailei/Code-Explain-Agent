package com.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Memory {
    private static final String FILE = "memory.json";
    private List<String> history = new ArrayList<>();
    private transient ObjectMapper mapper = new ObjectMapper();

    public void add(String role, String content) {
        history.add("[" + role + "] " + content);
        save();
    }

    public void addToolResult(String toolName, String result) {
        history.add("[tool:" + toolName + "] " + result);
        save();
    }

    public String summary() {
        return String.join("\n", history);
    }

    public void clear() {
        history.clear();
        new File(FILE).delete();
    }

    private void save() {
        try {
            ArrayNode arr = mapper.createArrayNode();
            for (String h : history) arr.add(h);
            ObjectNode root = mapper.createObjectNode();
            root.set("history", arr);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), root);
        } catch (Exception e) {
            System.err.println("⚠️ 记忆保存失败: " + e.getMessage());
        }
    }

    public static Memory load() {
        Memory m = new Memory();
        File f = new File(FILE);
        if (!f.exists()) return m;
        try {
            m.mapper.readTree(f).path("history").forEach(n -> m.history.add(n.asText()));
        } catch (Exception e) {
            System.err.println("⚠️ 记忆加载失败: " + e.getMessage());
        }
        return m;
    }
}