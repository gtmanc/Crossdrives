package com.example.crossdrives;

import android.content.Intent;
import android.view.View;

public abstract class SignInManager {
    public static final int Result_SUCCESS = 0;
    public static final int Result_FAILED = 1;

    static class Profile{
        String Name;
        String Mail;
        android.net.Uri PhotoUri;
    }

    //Callback gets called when asyn sign in is finished with or without error
    interface OnSilenceSignInfinished {
        //Sign in successfully if true is returned. Otherwise, false is returned.
        void onFinished(int result, Profile profile);
    }

    //start sign in flow. Mainly start the sign in activity.
    abstract boolean Start(View view, OnSilenceSignInfinished callback);

    // This method will be called as soon as onActivityResult is called by Android UI framwork.
    // Return Profile object if sign flow is done successfully.
    // NULL is returned if any of error occurred during sign in flow.
    //abstract Profile HandleSigninResult(Intent data);


    //Silence Sign in.
    //The sign in finished calback is always gets called
    abstract void silenceSignIn(OnSilenceSignInfinished callback);

}
