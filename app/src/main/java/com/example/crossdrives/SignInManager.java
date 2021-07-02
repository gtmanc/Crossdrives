package com.example.crossdrives;

import android.content.Intent;

public abstract class SignInManager {
    class Profile{
        String Name;
        String Mail;
        android.net.Uri PhotoUri;
    }

    //start sign in flow. Mainly start the sign in activity.
    abstract Intent Prepare();

    // This method will be called as soon as onActivityResult is called by Android UI framwork.
    // Return Profile object if sign flow is done successfully.
    // NULL is returned if any of error occurred during sign in flow.
    abstract Profile HandleSigninResult(Intent data);
}
