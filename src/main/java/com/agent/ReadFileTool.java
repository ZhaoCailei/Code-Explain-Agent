package com.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ReadFileTool implements Tool {
    @Override
    public String name() { return "read_file"; }

    @Override
    public String description() {
        return "读取指定路径的文本内容。参数: {\"path\": \"文件路径\"}";
    }

    @Override
    public String execute(String argsJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(argsJson);
            String path = node.has("path") ? node.get("path").asText() : "未知路径";

            // 模拟：真实场景用 Files.readString(Path.of(path))
            if (path.contains("Main.java")) {
                return "public class Main { public static void main(String[] a) { System.out.println(\"hello\"); } }";
            }
            return "[模拟文件内容] 路径=" + path + "，共 3 行代码。";
        } catch (Exception e) {
            return "读取失败: " + e.getMessage();
        }
    }
}