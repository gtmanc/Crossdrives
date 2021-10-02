package com.crossdrives.msgraph;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MSGraphHelper<T, Result> implements Callback<Result> {
    private String TAG = "CD.MSGraphHelper";
    private AbstractSnippet<T, Result> mItem;
    private final int ITEM_ARG = 1; //0: marker, 1:get me
    private Callback mCallback;

    public MSGraphHelper() {
    }

    public interface Callback{
        void onPhotoDownloaded(Bitmap bmp);
    }

    public boolean getMePhoto(Callback callback){
        boolean err = false;
        if(callback != null) {
            mCallback = callback;
            err = true;

            //SnippetContent.ITEMS.get(getArguments().getInt(ARG_ITEM_ID));
            mItem = (AbstractSnippet<T, Result>)SnippetContent.ITEMS.get(ITEM_ARG);

            // actually make the request
            mItem.request(mItem.mService, this);
        }
        else{
            Log.e(TAG, "callback should not be null!");
        }

        return err;
    }

    @Override
    public void onResponse(Call<Result> call, Response<Result> response) {
        Log.d(TAG, "[onResponse] Code: " + response.code());
        Log.d(TAG, "header: " + response.headers());

        try {
            byte[] body = ((ResponseBody) response.body()).bytes();
            Bitmap image = BitmapFactory.decodeByteArray(body, 0, body.length);

            Log.d(TAG, "Body: " + body);
            mCallback.onPhotoDownloaded(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(Call<Result> call, Throwable t) {
        Log.d(TAG, "[onFailure]: ");
    }
}
