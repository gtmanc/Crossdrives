package com.crossdrives.msgraph;

import android.app.Application;

import com.example.crossdrives.BuildConfig;
import com.sun.jersey.spi.inject.Inject;

import dagger.internal.DaggerCollections;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class SnippetApp extends Application {
    private static SnippetApp sSnippetApp;

    //public ObjectGraph mObjectGraph;

    @Inject
    protected String endpoint;

    @Inject
    protected HttpLoggingInterceptor.Level logLevel;

    @Inject
    protected Interceptor interceptor;

    public static SnippetApp getApp() {
        return sSnippetApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sSnippetApp = this;
        //We will replace dagger1 ObjectGraph with component
//        mObjectGraph = ObjectGraph.create(new AppModule());
//        mObjectGraph.inject(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public Retrofit getRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(logLevel);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
