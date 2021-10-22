package com.example.crossdrives;

import android.content.Intent;
import android.view.View;

public abstract class SignInManager{
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final String BRAND_GOOGLE = "Google";
    public static final String BRAND_MS = "MicroSoft";

    //Basic user profile information could be got as soon as app is signed in and get the API token successfully
    static class Profile{
        String Brand;
        String Name;
        String Mail;    //Not available right after microsoft sign in finished
        android.net.Uri PhotoUri; //Not available in microsoft sign in

    }

    /*The callback gets called when asyn sign in is finished with or without error
      Input
      result: sign in result. See RESULT_XXXXX in this class
      profile: user basic profile. Note this variants depending on brands.
      Object: instance used to build API client. e.g. drive access
    */
    interface OnInteractiveSignInfinished {
        void onFinished(int result, Profile profile, Object object);
    }

    interface OnSilenceSignInfinished {
        void onFinished(int result, Profile profile, Object object);
    }

    interface OnSignOutFinished {
        //Sign out successfully if true is returned. Otherwise, false is returned.
        void onFinished(int result);
    }

    //start interactive sign in flow. Mainly start the sign in activity.
    abstract boolean Start(View view, OnInteractiveSignInfinished callback);

    //Silence Sign in.
    abstract void silenceSignIn(OnSilenceSignInfinished callback);

    abstract void SignOut(OnSignOutFinished callback);
}
