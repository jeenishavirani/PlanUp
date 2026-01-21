package com.example.planup.ai;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {

    @POST("v1beta/models/gemini-flash-latest:generateContent")
    Call<ResponseBody> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest body
    );
}
