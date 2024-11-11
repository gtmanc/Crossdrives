package com.crossdrives.ui.document;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.crossdrives.cdfs.model.CdfsItem;

import java.util.List;

//https://stackoverflow.com/questions/46283981/android-viewmodel-additional-arguments
public class OpenTreeFactory implements ViewModelProvider.Factory {
    final String TAG = "CD.OpenTreeFactory";
    List<CdfsItem> list;

    public OpenTreeFactory(List<CdfsItem> parentList) {
        //Log.d(TAG, "OpenTreeFactory constructed.");
        list = parentList;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        //Log.d(TAG, "OpenTreeFactory create() gets called.");
        return (T) new OpenTree(list);
    }
}
