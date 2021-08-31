package com.example.crossdrives;

import android.content.Intent;

public abstract class SignInManager {
    class Profile{
        String Name;
        String Mail;
        android.net.Uri PhotoUri;
    }

    //Callback gets called when asyn sign in is finished with or without error
    interface OnSilenceSignInfinished {
        //Sign in successfully if true is returned. Otherwise, false is returned.
        void onFinished(boolean result, Profile profile);
    }

    //start sign in flow. Mainly start the sign in activity.
    abstract Intent Start();

    // This method will be called as soon as onActivityResult is called by Android UI framwork.
    // Return Profile object if sign flow is done successfully.
    // NULL is returned if any of error occurred during sign in flow.
    abstract Profile HandleSigninResult(Intent data);


    //Silence Sign in.
    //The sign in finished calback is always gets called
    abstract void silenceSignIn(OnSilenceSignInfinished callback);

}
