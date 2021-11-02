package com.example.crossdrives;

public abstract class DriveClient {

    /*
    SignInAccount: account used to create drive client. It contains the necessary information to
    request user to perform sign in.
     */
    abstract DriveClient create(Object SignInAccount);

}
