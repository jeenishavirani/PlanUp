package com.example.planup.ai.gemini;

import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.ListenableFuture;

public class GeminiApiClient {

    private static final String TAG = "GeminiApiClient";
    private static final String API_KEY = "xxx";
    private static GenerativeModelFutures modelFutures;

    public static synchronized GenerativeModelFutures getModel() {
        if (modelFutures == null) {
            try {
                Log.d(TAG, "Initializing Gemini Model...");
                
                // 🔹 Standard config that definitely works in 0.9.0
                GenerationConfig config = new GenerationConfig.Builder()
                        .build();

                GenerativeModel gm = new GenerativeModel(
                        "gemini-1.5-flash",
                        API_KEY,
                        config
                );
                
                modelFutures = GenerativeModelFutures.from(gm);
                Log.d(TAG, "Gemini Client Initialized Successfully");
            } catch (Exception e) {
                Log.e(TAG, "FATAL: Initialization Failed: " + e.getMessage());
            }
        }
        return modelFutures;
    }

    public static ListenableFuture<GenerateContentResponse> sendMessage(String prompt) {
        try {
            GenerativeModelFutures model = getModel();
            if (model == null) {
                Log.e(TAG, "Cannot send message: Model is null");
                return null;
            }

            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();
            
            Log.d(TAG, "Sending prompt: " + prompt);
            return model.generateContent(content);
        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage: " + e.getMessage());
            return null;
        }
    }
}
