package com.crossdrives.msgraph;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;
import okhttp3.Response;
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
//    @Module(library = true,
//            injects = {SnippetApp.class}
//    )

    public static final String PREFS = "com.microsoft.o365_android_unified_API_REST_snippets";

    @Provides
    @SuppressWarnings("unused") // not actually unused -- used by Dagger
    public String providesRestEndpoint() {
        return ServiceConstants.AUTHENTICATION_RESOURCE_ID;
    }

    @Provides
    @SuppressWarnings("unused") // not actually unused -- used by Dagger
    public HttpLoggingInterceptor.Level providesLogLevel() {
        return HttpLoggingInterceptor.Level.BODY;
    }

    @Provides
    @SuppressWarnings("unused") // not actually unused -- used by Dagger
    public Interceptor providesRequestInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                // apply the Authorization header if we had a token...
                final SharedPreferences preferences
                        = SnippetApp.getApp().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                final String token =
                        preferences.getString(SharedPrefsUtil.PREF_AUTH_TOKEN, null);

                Request request = chain.request();
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        // This header has been added to identify this sample in the Microsoft Graph service.
                        // If you're using this code for your project please remove the following line.
                        //.addHeader("SampleID", "android-java-snippets-rest-sample")
                        .build();
                Response response = chain.proceed(request);
                return response;
            }
        };
    }
}
