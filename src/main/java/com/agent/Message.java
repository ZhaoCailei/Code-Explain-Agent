package com.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private String role;
    private String content;
    private JsonNode tool_calls;       // assistant 返回的工具调用
    private String tool_call_id;       // tool 角色回传时用的 ID
    private String name;               // tool 角色名称

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    // getters & setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public JsonNode getTool_calls() { return tool_calls; }
    public void setTool_calls(JsonNode tool_calls) { this.tool_calls = tool_calls; }
    public String getTool_call_id() { return tool_call_id; }
    public void setTool_call_id(String tool_call_id) { this.tool_call_id = tool_call_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}