package com.example.planup.ai;

import java.util.ArrayList;
import java.util.List;

public class GeminiRequest {
    public List<Content> contents;
    public Content system_instruction;
    public GenerationConfig generationConfig;

    public GeminiRequest(String prompt, String systemInstruction) {
        this.contents = new ArrayList<>();
        Content content = new Content();
        content.parts = new ArrayList<>();
        Part part = new Part();
        part.text = prompt;
        content.parts.add(part);
        this.contents.add(content);

        if (systemInstruction != null) {
            this.system_instruction = new Content();
            this.system_instruction.parts = new ArrayList<>();
            Part systemPart = new Part();
            systemPart.text = systemInstruction;
            this.system_instruction.parts.add(systemPart);
        }

        this.generationConfig = new GenerationConfig();
        this.generationConfig.response_mime_type = "application/json";
    }

    public static class Content {
        public List<Part> parts;
    }

    public static class Part {
        public String text;
    }

    public static class GenerationConfig {
        public String response_mime_type;
    }
}
