package com.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private List<Choice> choices;

    public ChatResponse() {}
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public static class Choice {
        private Message message;
        private Integer index;
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
    }

    public String getFirstContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return "";
    }
}