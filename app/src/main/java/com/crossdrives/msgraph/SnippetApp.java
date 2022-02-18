package com.crossdrives.msgraph;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.crossdrives.BuildConfig;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.HiltAndroidApp;
import dagger.internal.DaggerCollections;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;
@HiltAndroidApp
public class SnippetApp extends Application {
    private static String TAG = "CD.SnippetApp";
    private static SnippetApp sSnippetApp;

    //public ObjectGraph mObjectGraph;

    @Inject
    protected String endpoint;

    @Inject
    protected HttpLoggingInterceptor.Level logLevel;

    @Inject
    protected Interceptor interceptor;

    static private Context mContext;

    public static SnippetApp getApp() {
        //Log.d(TAG, "getApp");
        return sSnippetApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.d(TAG, "onCreate");
        sSnippetApp = this;
        //We will replace dagger1 ObjectGraph with component
//        mObjectGraph = ObjectGraph.create(new AppModule());
//        mObjectGraph.inject(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        mContext = getApplicationContext();
    }

    public Retrofit getRetrofit() {
        Log.d(TAG, "getRetrofit");
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

    static public Context getAppContext(){
        return mContext;
    }
}
