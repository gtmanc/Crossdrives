package com.example.crossdrives;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SearchResultsActivity extends AppCompatActivity {
    private String TAG = "CD.SearchResultsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreated");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_results);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handleIntent(getIntent());
    }

    //Handle the ACTION_SEARCH intent in the onNewIntent() method searchable activity launches in single top mode (android:launchMode="singleTop"),
    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent");

      super.onNewIntent(intent);
      handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
            Log.d(TAG, "query: " + query);
            suggestions.saveRecentQuery(query, null);
        }
    }
}
