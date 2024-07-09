package com.crossdrives.signin.microsoft;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

//Implement based on https://blog.mastykarz.nl/easily-debug-microsoft-graph-java-sdk-requests/
public class DebugHandler implements okhttp3.Interceptor{
    final String TAG = "CD.DebugHandler";
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Log.d(TAG, "");//System.out.println("");
        Request originalReq = chain.request();
        Log.d(TAG, "Request: " + originalReq.method() + " " + originalReq.url().toString());//System.out.printf("Request: %s %s%n", chain.request().method(), chain.request().url().toString());
        Log.d(TAG, "Request headers:");//System.out.println("Request headers:");
        originalReq.headers().toMultimap()
                .forEach((k, v) -> Log.d(TAG, k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        if (originalReq.body() != null) {
            Log.d(TAG, "Request body:");//System.out.println("Request body:");
            final Buffer buffer = new Buffer();
            originalReq.body().writeTo(buffer);
            Log.d(TAG, buffer.readString(StandardCharsets.UTF_8));//System.out.println(buffer.readString(StandardCharsets.UTF_8));
        }

        // A workaround for PUT when upload session is used (resumable upload for ms graph)
        // The token must not be included in the PUT request header. i.e. 401 error code received
        //https://stackoverflow.com/questions/32963394/how-to-use-interceptor-to-add-headers-in-retrofit-2-0
        Request newReq;
        String contentRange = originalReq.header("Content-Range");
        if(contentRange == null){
            newReq = originalReq;
        }
        else{
            Log.d(TAG, "Content range = " + contentRange.toString());
            Log.d(TAG, "remove authorizationBearer");
            newReq= originalReq.newBuilder()
                    .removeHeader("Authorization")
                    .build();

            Log.d(TAG, "");//System.out.println("");
            Log.d(TAG, "New Request: " + newReq.method() + " " + newReq.url().toString());//System.out.printf("Request: %s %s%n", chain.request().method(), chain.request().url().toString());
            Log.d(TAG, "New Request headers:");//System.out.println("Request headers:");
            newReq.headers().toMultimap()
                    .forEach((k, v) -> Log.d(TAG, k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        }

        final Response response = chain.proceed(newReq);

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