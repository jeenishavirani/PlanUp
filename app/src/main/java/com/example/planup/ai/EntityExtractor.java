package com.example.planup.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityExtractor {

    public static Map<String, String> extract(String input) {

        Map<String, String> entities = new HashMap<>();
        input = input.toLowerCase();

        // 🔢 Extract days (e.g. "15 days")
        Pattern dayPattern = Pattern.compile("(\\d+)\\s*day");
        Matcher dayMatcher = dayPattern.matcher(input);
        if (dayMatcher.find()) {
            entities.put("days", dayMatcher.group(1));
        }

        // ⏰ Extract hours (e.g. "3 hours")
        Pattern hourPattern = Pattern.compile("(\\d+)\\s*hour");
        Matcher hourMatcher = hourPattern.matcher(input);
        if (hourMatcher.find()) {
            entities.put("hours", hourMatcher.group(1));
        }

        // 🎓 Exam keyword
        if (input.contains("exam")) {
            entities.put("type", "exam");
        }

        // 🥗 Diet keyword
        if (input.contains("diet") || input.contains("weight")) {
            entities.put("type", "diet");
        }

        // 🤰 Pregnancy keyword
        if (input.contains("pregnant") || input.contains("pregnancy")) {
            entities.put("type", "pregnancy");
        }

        // 🧠 Focus keyword
        if (input.contains("focus") || input.contains("concentration")) {
            entities.put("type", "focus");
        }

        return entities;
    }
}
