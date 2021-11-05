package com.crossdrives.driveclient;

import android.content.Context;
import android.os.Build;

import com.google.common.io.Files;

public abstract class DriveClient {

    /*
    SignInAccount: account used to create drive client. It contains the necessary information to
    request user to perform sign in.
     */
    //abstract DriveClient create(Object SignInAccount);

    public abstract static Builder {

        public abstract Builder putContext(Context context);

        public abstract DriveClient build(Object auth);
    }

    public abstract class Query{
        public abstract void getFiles(IQueryCallback callback);
    }

    interface IQueryCallback{
        void onFinished(Files files);
    }

}
