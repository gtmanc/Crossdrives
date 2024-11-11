package com.crossdrives.signin;

public interface ISignOutFinishedListener {
    //Sign out successfully if true is returned. Otherwise, false is returned.
    void onFinished(int result, String brand);
}
