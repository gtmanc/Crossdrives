package com.example.crossdrives;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MasterAccountFragment extends Fragment {
    private String TAG = "CD.MasterAccountFragment";
    List<AccountListModel> mAccountList = new ArrayList<>();
    Fragment mFragment;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.master_account_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String MyArg = MasterAccountFragmentArgs.fromBundle(getArguments()).getCreateAccountName();
        if(MyArg != null)
            Toast.makeText(getContext(), "Master Account Added: " + MyArg, Toast.LENGTH_LONG).show();

        readAllAccounts();

        udpateProfiles(view);

        view.findViewById(R.id.add_account_btn).setOnClickListener(listener_account_add);
        mFragment = FragmentManager.findFragment(view);

        Toolbar toolbar = view.findViewById(R.id.master_accounts_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);

    }

    private View.OnClickListener listener_account_add = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Fragment f = FragmentManager.findFragment(v);
            NavDirections a = MasterAccountFragmentDirections.navigateToAddAccount();
            NavHostFragment.findNavController(f).navigate(a);
        }
    };

    /*
        Use the information stored in mAccountList to update the user profile content shown in screen.
        The items in the user profile card view will be updated one by one according to the record(row) queried from database.
    */
    private void udpateProfiles(View v){
        v.findViewById(R.id.iv_info_no_account).setVisibility(View.GONE);
        v.findViewById(R.id.tv_info_no_account_available).setVisibility(View.GONE);
        v.findViewById(R.id.account_list).setVisibility(View.VISIBLE);
        if(mAccountList.size() > 0){
            Log.d(TAG, "Start to download image");
            for(int i = 0; i < mAccountList.size(); i++) {
                ImageView iv = v.findViewById(R.id.account_brand_profile_image);
                iv.setImageResource(R.drawable.logo_drive_2020q4_color_2x_web_64dp);
                TextView t = v.findViewById(R.id.account_name);
                t.setText(mAccountList.get(i).getName());
                t = v.findViewById(R.id.account_mail);
                t.setText(mAccountList.get(i).getMail());
                new DownloadPhoto(v.findViewById(R.id.account_profile_image))
                        .execute(mAccountList.get(0).getPhotoUrl().toString());
            }
        }
        else{
            v.findViewById(R.id.iv_info_no_account).setVisibility(View.VISIBLE);
            v.findViewById(R.id.tv_info_no_account_available).setVisibility(View.VISIBLE);
            v.findViewById(R.id.account_list).setVisibility(View.GONE);
        }
    }

    private class DownloadPhoto extends AsyncTask<String, Void, Bitmap>{
        ImageView mImageView;
        public DownloadPhoto(ImageView iv) {
            mImageView = iv;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap bm = null;
            try{
                InputStream in = new java.net.URL(urls[0]).openStream();
                bm = BitmapFactory.decodeStream(in);
            }catch(Exception e){
                Log.w(TAG, "Open URL failed");
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mImageView.setImageBitmap(bitmap);
        }
    }
    /*
    Read all accounts from database and save to mAccountList.
     */
    private void readAllAccounts(){
        Cursor c;
        String ColNames[];
        DBHelper dbh = new DBHelper(getContext(), null,null,0);

        c = dbh.query();
        if(c.getCount() == 0){
            Log.d(TAG, "Number of queried account is 0");
        }
        else{
            Log.d(TAG, "Count: " + Integer.toString(c.getCount()));
//            Log.d(TAG, "Column count: " + Integer.toString(c.getColumnCount()));
//            ColNames = c.getColumnNames();
//            for(int i = 0; i <ColNames.length; i++){
//                Log.d(TAG, "Number: " + ColNames[i]);
//            }

            int ibrand = DBConstants.COL_INDX_BRAND;
            int iname = DBConstants.COL_INDX_NAME;
            int imail = DBConstants.COL_INDX_MAIL;
            int iphoto = DBConstants.COL_INDX_PHOTOURL;
            c.moveToFirst();
            do{
                URL url;
                try {
                    url = new URL(c.getString(iphoto));
                }catch (MalformedURLException e) {
                    Log.w(TAG, "generating URL fails ");
                    continue;
                }
//                Log.d(TAG, "Brand: " + c.getString(ibrand));
                AccountListModel list = new AccountListModel(c.getString(ibrand), c.getString(iname), c.getString(imail),url);
                mAccountList.add(list);
            }while(c.moveToNext());
        }

        dbh.close();
        c.close();
    }

    private void updateCardContent(String name, String mail, Uri photourl){

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_option, menu);

        menu.findItem(R.id.search).setVisible(false);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        Log.d(TAG, "onOptionsItemSelected");

        //Because we only have a action button (close Button) is action bar, so simply go back to previous screen (query result screen)
        NavDirections a = MasterAccountFragmentDirections.navigateBackToQueryResult();
        NavHostFragment.findNavController(mFragment).navigate(a);

        return super.onOptionsItemSelected(item);
    }
}
