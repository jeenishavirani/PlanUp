package com.example.planup.ai;

import java.util.List;

public class ChatResponse {

    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String role;
        public String content;
    }

}