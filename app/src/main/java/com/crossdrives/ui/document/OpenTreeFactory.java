package com.crossdrives.ui.document;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.crossdrives.cdfs.model.CdfsItem;

import java.util.List;

//https://stackoverflow.com/questions/46283981/android-viewmodel-additional-arguments
public class OpenTreeFactory implements ViewModelProvider.Factory {
    List<CdfsItem> list;

    public OpenTreeFactory(List<CdfsItem> parentList) {
        list = parentList;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new OpenTree(list);
    }
}
