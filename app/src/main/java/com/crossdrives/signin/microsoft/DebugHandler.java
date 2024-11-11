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
    Printer printer = new Printer();
    
    private class Printer{
        boolean enable = true;
        void po(String message){
            if(enable) Log.d(TAG, message);
        };
        
        void setEnable(boolean enable){ this.enable = enable;}
    }

    public DebugHandler() {
        printer.enable = false;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        printer.po("");//System.out.println("");
        Request originalReq = chain.request();
        printer.po("Request: " + originalReq.method() + " " + originalReq.url().toString());//System.out.printf("Request: %s %s%n", chain.request().method(), chain.request().url().toString());
        printer.po("Request headers:");//System.out.println("Request headers:");
        originalReq.headers().toMultimap()
                .forEach((k, v) -> printer.po( k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        if (originalReq.body() != null) {
            printer.po("Request body:");//System.out.println("Request body:");
            final Buffer buffer = new Buffer();
            originalReq.body().writeTo(buffer);
            printer.po(buffer.readString(StandardCharsets.UTF_8));//System.out.println(buffer.readString(StandardCharsets.UTF_8));
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
            printer.po( "Content range = " + contentRange.toString());
            printer.po( "remove authorizationBearer");
            newReq= originalReq.newBuilder()
                    .removeHeader("Authorization")
                    .build();

            printer.po( "");//System.out.println("");
            printer.po( "New Request: " + newReq.method() + " " + newReq.url().toString());//System.out.printf("Request: %s %s%n", chain.request().method(), chain.request().url().toString());
            printer.po( "New Request headers:");//System.out.println("Request headers:");
            newReq.headers().toMultimap()
                    .forEach((k, v) -> printer.po( k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        }

        final Response response = chain.proceed(newReq);

        printer.po( "");//System.out.println("");
        printer.po( "Response:　" + response.code());//System.out.printf("Response: %s%n", response.code());
        printer.po( "Response headers:");//System.out.println("Response headers:");
        response.headers().toMultimap()
                .forEach((k, v) -> printer.po( k + String.join(", ", v)));//System.out.printf("%s: %s%n", k, String.join(", ", v)));
        if (response.body() != null) {
            printer.po( "Response body:");//System.out.println("Response body:");
            printer.po( response.peekBody(Long.MAX_VALUE).string());//System.out.println(response.peekBody(Long.MAX_VALUE).string());
        }

        return response;
    }
}