package com.crossdrives.ui.document;

import androidx.lifecycle.ViewModel;

/*
    Design hint from Google:
    A ViewModel usually shouldn't reference a view, Lifecycle, or any class that may hold a reference to the activity context.
    Because the ViewModel lifecycle is larger than the UI's, holding a lifecycle-related API in the ViewModel could cause memory leaks.
 */
public class FetchList extends ViewModel {
    final String TAG = "FetchList";

}
