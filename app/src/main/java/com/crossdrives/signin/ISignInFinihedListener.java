package com.crossdrives.signin;
/*
    The callback gets called when the requested operation is finished with or without error
*/
public interface ISignInFinihedListener {
    /*
        Profile: user basic profile. Note this variants depending on brands.
        token: AccessToken can be used to build API client
     */
     void onFinished(ISignInRequest.Profile profile, String token);

     void onFailure(String brand, String err);
}
