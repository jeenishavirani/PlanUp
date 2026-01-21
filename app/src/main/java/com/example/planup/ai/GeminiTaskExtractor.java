package com.example.planup.ai;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiTaskExtractor {

    private static final String TAG = "GeminiTaskExtractor";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    
    private static GeminiApiService service;
    private static final Gson gson = new Gson();

    public interface ExtractionCallback {
        void onResult(TaskResult result);
        void onError(String error);
    }

    public static class TaskResult {
        public boolean isTask;
        public String title;
        public String date;
        public String time;
        public String reply;
    }

    private static synchronized GeminiApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new RetryInterceptor())
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(GeminiApiService.class);
        }
        return service;
    }

    private static class RetryInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            okhttp3.Response response = null;
            int retryCount = 0;
            int maxRetries = 3;
            long delay = 1000;

            while (retryCount <= maxRetries) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }
                    if (response.code() != 429 && response.code() < 500) {
                        return response;
                    }
                } catch (IOException e) {
                    if (retryCount >= maxRetries) throw e;
                }

                if (response != null) response.close();
                
                retryCount++;
                try {
                    Log.d(TAG, "Retrying request... attempt " + retryCount);
                    Thread.sleep(delay);
                    delay *= 2;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
            return response;
        }
    }

    public static void extractTask(String userMessage, String apiKey, ExtractionCallback callback) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String systemInstruction = "You are PlanUp AI assistant. Today is " + currentDate + ".\n" +
                "Extract task details from user messages. Output MUST be valid JSON with this structure:\n" +
                "{\"isTask\":boolean, \"title\":string, \"date\":string, \"time\":string, \"reply\":string}\n" +
                "Rules:\n" +
                "1. isTask=true only if user wants to schedule/add a task.\n" +
                "2. Extract date as YYYY-MM-DD and time as HH:mm if mentioned.\n" +
                "3. If not a task, provide a friendly reply in the 'reply' field.";

        GeminiRequest geminiRequest = new GeminiRequest(userMessage, systemInstruction);

        getService().generateContent(apiKey, geminiRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String raw = response.body().string();
                        JsonObject json = gson.fromJson(raw, JsonObject.class);
                        
                        JsonArray candidates = json.getAsJsonArray("candidates");
                        if (candidates != null && candidates.size() > 0) {
                            String aiText = candidates.get(0).getAsJsonObject()
                                    .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                                    .get("text").getAsString();

                            String cleanedJson = cleanJson(aiText);
                            callback.onResult(gson.fromJson(cleanedJson, TaskResult.class));
                        } else {
                            callback.onError("I couldn't generate a response. Please try rephrasing your request.");
                        }
                    } else {
                        handleErrorResponse(response, callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse Error", e);
                    callback.onError("I'm having trouble processing that right now. Please try again in a moment.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network failure", t);
                if (t instanceof java.net.SocketTimeoutException) {
                    callback.onError("Connection timed out. Please try again when your connection is stronger.");
                } else if (t instanceof java.net.UnknownHostException) {
                    callback.onError("No internet connection detected. Please check your signal.");
                } else {
                    callback.onError("I'm having trouble connecting. Please check your network and try again.");
                }
            }
        });
    }

    private static void handleErrorResponse(Response<ResponseBody> response, ExtractionCallback callback) {
        int code = response.code();
        Log.e(TAG, "API Error: " + code);
        switch (code) {
            case 401:
            case 403:
                callback.onError("There's an issue with my AI configuration. Please contact support.");
                break;
            case 429:
                callback.onError("I'm receiving too many requests. Please wait a few seconds before trying again.");
                break;
            case 500:
            case 503:
            case 504:
                callback.onError("The AI servers are currently busy. Please try again in a moment.");
                break;
            default:
                callback.onError("I'm experiencing some technical difficulties (Error " + code + "). Please try again later.");
                break;
        }
    }

    private static String cleanJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
