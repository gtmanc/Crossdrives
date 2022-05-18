package com.example.crossdrives;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

public abstract class SignInManager{
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final String BRAND_GOOGLE = "Google";
    public static final String BRAND_MS = "Microsoft";

    //Basic user profile information could be got as soon as app is signed in and get the API token successfully
    static class Profile{
        String Brand;
        String Name;
        String Mail;    //Not available right after microsoft sign in finished
        android.net.Uri PhotoUri; //Not available in microsoft sign in

    }

    /*
    The callback gets called when the requested operation is finished with or without error
     */
    interface OnSignInfinished {
        /*
        Profile: user basic profile. Note this variants depending on brands.
        token: AccessToken can be used to build API client
         */
        void onFinished(Profile profile, String token);

        void onFailure(String err);
    }

    interface OnSignOutFinished {
        //Sign out successfully if true is returned. Otherwise, false is returned.
        void onFinished(int result, String brand);
    }
    interface OnPhotoDownloaded {
        void onDownloaded(Bitmap bmp, Object object);
    }

    //Operations provided
    //start interactive sign in flow. Mainly start the sign in activity.
    abstract boolean Start(Activity activity, OnSignInfinished callback);

    //Silence Sign in.
    abstract void silenceSignIn(Activity activity, OnSignInfinished callback);

    //Sign out user
    abstract void SignOut(OnSignOutFinished callback);

    //gt user photo
    abstract void getPhoto(Object object, OnPhotoDownloaded callback);
}
