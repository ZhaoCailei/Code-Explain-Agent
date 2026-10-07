package com.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private String id;
    private String object;
    private List<Choice> choices;

    public ChatResponse() {}

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public static class Choice {
        private Integer index;
        private Message message;
        private String finish_reason;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public String getFinish_reason() { return finish_reason; }
        public void setFinish_reason(String finish_reason) { this.finish_reason = finish_reason; }
    }

    /**
     * 获取第一个 choice 的文本内容
     */
    public String getFirstContent() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            if (msg != null && msg.getContent() != null) {
                return msg.getContent();
            }
        }
        return "";
    }

    /**
     * 获取第一个 choice 的 tool_calls（标准格式）
     */
    public JsonNode getFirstToolCalls() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            if (msg != null && msg.getTool_calls() != null) {
                return msg.getTool_calls();
            }
        }
        return null;
    }
}