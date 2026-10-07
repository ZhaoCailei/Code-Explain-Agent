package com.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(argsJson);
            String path = node.has("path") ? node.get("path").asText() : "";

            if (path.isBlank()) {
                return "错误：未提供文件路径";
            }

            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                return "错误：文件不存在 → " + path;
            }
            if (Files.isDirectory(p)) {
                return "错误：路径是目录而非文件 → " + path;
            }

            String content = Files.readString(p);
            // 限制返回长度，防止超 token
            if (content.length() > 8000) {
                content = content.substring(0, 8000) + "\n\n... (内容过长，已截断)";
            }
            return content;

        } catch (Exception e) {
            return "读取失败: " + e.getMessage();
        }
    }
}