package com.example.crossdrives;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public class DeleteFileActivity extends AppCompatActivity {
    private String TAG = "CD.DeleteFileActivity";
    Intent mIntent = new Intent();
        List<ItemModelBase> mItems;
    DeleteFileAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_result);
        Bundle bundle = this.getIntent().getExtras();
        //ListView listview = (ListView) findViewById(R.id.listview_query);

        Log.d(TAG, "onCreated");
        //        ArrayAdapter adapter = new ArrayAdapter(this,
//                android.R.layout.simple_list_item_1,
//                mList);
        mItems = new ArrayList<>();
        mItems.add(new ItemModelBase(false, "File A", null));
        mItems.add(new ItemModelBase(false, "File B", null));
        mAdapter = new DeleteFileAdapter(this, mItems);
//        listview.setAdapter(mAdapter);
//        listview.setOnItemClickListener(onClickListView);
        setResult(RESULT_OK, mIntent);
    }

    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //mIntent.putExtra("SelectedFiles", mList.get(position));
            // Toast 快顯功能 第三個參數 Toast.LENGTH_SHORT 2秒  LENGTH_LONG 5秒
            //Toast.makeText(QueryResultActivity.this,"點選第 "+(position +1) +" 個 \n內容：" + mList.get(position), Toast.LENGTH_SHORT).show();

            ItemModelBase model = mItems.get(position);

            if (model.isSelected())
                model.setSelected(false);

            else
                model.setSelected(true);

            mItems.set(position, model);

            //now update adapter
            mAdapter.updateRecords(mItems);
            setResult(RESULT_OK, mIntent);
        }
    };
}
