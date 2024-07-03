package com.crossdrives.signin.microsoft;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Response;
import okio.Buffer;

//Implement based on https://blog.mastykarz.nl/easily-debug-microsoft-graph-java-sdk-requests/
public class DebugHandler implements okhttp3.Interceptor{
    final String TAG = "CD.DebugHandler";
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Log.d(TAG, "");//System.out.println("");
        Log.d(TAG, "Request: " + chain.request().method() + " " + chain.request().url().toString());//System.out.printf("Request: %s %s%n", chain.request().method(), chain.request().url().toString());
        Log.d(TAG, "Request headers:");//System.out.println("Request headers:");
        chain.request().headers().toMultimap()
                .forEach((k, v) -> Log.d(TAG, k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        if (chain.request().body() != null) {
            Log.d(TAG, "Request body:");//System.out.println("Request body:");
            final Buffer buffer = new Buffer();
            chain.request().body().writeTo(buffer);
            Log.d(TAG, buffer.readString(StandardCharsets.UTF_8));//System.out.println(buffer.readString(StandardCharsets.UTF_8));
        }

        final Response response = chain.proceed(chain.request());

        Log.d(TAG, "");//System.out.println("");
        Log.d(TAG, "Response:　" + response.code());//System.out.printf("Response: %s%n", response.code());
        Log.d(TAG, "Response headers:");//System.out.println("Response headers:");
        response.headers().toMultimap()
                .forEach((k, v) -> Log.d(TAG, k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        if (response.body() != null) {
            Log.d(TAG, "Response body:");//System.out.println("Response body:");
            Log.d(TAG, response.peekBody(Long.MAX_VALUE).string());//System.out.println(response.peekBody(Long.MAX_VALUE).string());
        }

        return response;
    }
}