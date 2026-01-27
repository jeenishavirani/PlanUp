package com.example.planup.ai;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
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
    private static final Random random = new Random();

    public interface ExtractionCallback {
        void onResult(ExtractionResult result);
        void onError(String error);
    }

    public static class TaskResult {
        public String title;
        public String description;
        public String date;
        public String time;
        public String priority;
        public String category;
        public boolean wantsReminder;
    }

    public static class ExtractionResult {
        public boolean hasTasks;
        public List<TaskResult> tasks = new ArrayList<>();
        public String reply;
    }

    private static synchronized GeminiApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            
            // Ultra-stable Dispatcher for high-end performance
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequestsPerHost(10);

            OkHttpClient client = new OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .addInterceptor(logging)
                    .addInterceptor(new AdvancedRetryInterceptor())
                    .connectTimeout(45, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS) // Deep processing time
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(5, 10, TimeUnit.MINUTES))
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

    /**
     * Advanced Interceptor with Jittered Exponential Backoff
     */
    private static class AdvancedRetryInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            int retryCount = 0;
            int maxRetries = 6; // High-end persistence
            long baseDelay = 1200;

            while (true) {
                try {
                    okhttp3.Response response = chain.proceed(request);
                    
                    // Success or client-side error (no point retrying 400s)
                    if (response.isSuccessful() || (response.code() >= 400 && response.code() < 500 && response.code() != 429)) {
                        return response;
                    }

                    response.close();
                } catch (IOException e) {
                    if (retryCount >= maxRetries) throw e;
                    Log.w(TAG, "Network handshake failed, attempt " + (retryCount + 1));
                }

                retryCount++;
                if (retryCount > maxRetries) {
                    throw new IOException("Aborting after " + maxRetries + " attempts.");
                }

                // Jittered Exponential Backoff calculation
                long jitter = random.nextInt(800);
                long delay = (long) Math.pow(2, retryCount) * baseDelay + jitter;
                
                try {
                    Log.d(TAG, "Stabilizing connection... Retrying in " + delay + "ms");
                    Thread.sleep(Math.min(delay, 25000)); // Cap at 25s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }
        }
    }

    public static void extractTask(String userMessage, String apiKey, ExtractionCallback callback) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Streamlined System Instruction for faster AI "Cold Start"
        String systemInstruction = "Act as PlanUp ssistant. Today:" + currentDate + ".\n" +
                "Output ONLY JSON.\n" +
                "Structure: {\"hasTasks\":bool, \"tasks\":[{\"title\":str, \"description\":str, \"date\":str, \"time\":str, \"priority\":str, \"category\":str, \"wantsReminder\":bool}], \"reply\":str}\n" +
                "Rules: date=YYYY-MM-DD, time=HH:mm, priority=High/Medium/Low, wantsReminder=true if requested.";

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

                            callback.onResult(gson.fromJson(cleanJson(aiText), ExtractionResult.class));
                        } else {
                            callback.onError("AI Engine Warmup required. Please try again.");
                        }
                    } else {
                        handleError(response.code(), callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Payload Parse Error", e);
                    callback.onError("Data sync error. Rephrasing might help!");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Critical Network Failure", t);
                callback.onError("System is currently offline. Please check your signal 📶");
            }
        });
    }

    private static void handleError(int code, ExtractionCallback callback) {
        if (code == 429) callback.onError("Server busy (Rate Limit). Waiting for clearance...");
        else if (code >= 500) callback.onError("AI Cluster is rebooting. One moment...");
        else callback.onError("Connection lost (Error " + code + "). Check internet.");
    }

    private static String cleanJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.contains("{")) {
            text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
        }
        return text;
    }
}
