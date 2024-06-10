package com.crossdrives.signin;

import android.app.Activity;

public interface ISignInRequest {
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final String BRAND_GOOGLE = "Google";
    public static final String BRAND_MS = "Microsoft";

    //Basic user profile information could be got as soon as app is signed in and get the API token successfully
    public class Profile{
        public String Brand;
        public String Name;
        public String Mail;    //Not available right after microsoft sign in finished
        public android.net.Uri PhotoUri; //Not available in microsoft sign in

    }

    //Operations provided
    //start interactive sign in flow. Mainly start the sign in activity.
    public boolean Start(Activity activity, ISignInFinihedListener callback);

    //Silence Sign in.
    public void silenceSignIn(Activity activity, ISignInFinihedListener callback);

    //Sign out user
    public void SignOut(ISignOutFinishedListener callback);

    //gt user photo
    public void getPhoto(Object object, IPhotoDownloadedListener callback);
}
