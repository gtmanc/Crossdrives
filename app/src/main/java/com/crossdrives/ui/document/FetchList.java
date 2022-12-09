package com.crossdrives.ui.document;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Result;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.example.crossdrives.SerachResultItemModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;
import java.util.List;

/*
    Design hint from Google:
    A ViewModel usually shouldn't reference a view, Lifecycle, or any class that may hold a reference to the activity context.
    Because the ViewModel lifecycle is larger than the UI's, holding a lifecycle-related API in the ViewModel could cause memory leaks.
 */
public class FetchList extends ViewModel {
    final String TAG = "FetchList";

}
