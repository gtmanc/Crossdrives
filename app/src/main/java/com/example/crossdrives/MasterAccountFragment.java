package com.example.crossdrives;

import android.graphics.Bitmap;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MasterAccountFragment extends BaseFragment {
    private String TAG = "CD.MasterAccountFragment";
    private List<AccountManager.AccountInfo> mAi = new ArrayList<>();
    //private View mView;
    private Fragment mFragment;
    private List<CardView> mLayoutCards = new ArrayList<>();
    private HashMap<String, Integer> mLogoResIDs= new HashMap<>();
    private int mCardIndex;
    private ImageView mIvUserPhoto;
    private Bitmap mBmpUserPhoto;
    List<String> mBrands = GlobalConstants.BrandList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //allocate space for the list so that we can directly add the value with index later
//        for(String s : BrandList) {
//            mLayoutCards.add(null);
//        }
    }

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
        if(MyArg != null && MyArg != "NoName")
            Toast.makeText(getContext(), "Master Account Added: " + MyArg, Toast.LENGTH_LONG).show();

        readAllAccounts();

        prepareUI(view);

        udpateCards(view);

        view.findViewById(R.id.add_account_btn).setOnClickListener(listener_account_add);
        mFragment = FragmentManager.findFragment(view);

        Toolbar toolbar = view.findViewById(R.id.master_accounts_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);

    }

    private void prepareUI(View v){
        CardView iv = v.findViewById(R.id.account_list1); mLayoutCards.add(0, iv);
        iv = v.findViewById(R.id.account_list2); mLayoutCards.add(1, iv);

        mLogoResIDs.put(mBrands.get(0), new Integer(R.drawable.logo_drive_2020q4_color_2x_web_64dp));
        mLogoResIDs.put(mBrands.get(1), new Integer(R.drawable.onedrive_logo_wine));
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
        Use the information stored in mAccountList to update the user profile content shown in the card.
        The items in the user profile card view will be updated one by one according to the record(row)
        queried from database.
    */
    private void udpateCards(View v){
        //Show no account message or not
        showNoAccountMsg(v);
        setCardInVisible(0);
        setCardInVisible(1);
        if(mAi.size() > 0){
            RemoveNoAccountMsg(v);
        }

        //if there is activated account, start to update the card content.
        for(int i = 0; i < mAi.size(); i++){
            setCardVisible(i);
            updateLogo(i, mAi.get(i).brand);

            updateName(i);

            updateMail(i);

            updatePhoto(i, mAi.get(i).brand);
        }

    }
    private void setCardVisible(int index){
        mLayoutCards.get(index).setVisibility(View.VISIBLE);
    }
    private void setCardInVisible(int index){
        mLayoutCards.get(index).setVisibility(View.GONE);
    }
    private void showNoAccountMsg(View v){
        v.findViewById(R.id.iv_info_no_account).setVisibility(View.VISIBLE);
        v.findViewById(R.id.tv_info_no_account_available).setVisibility(View.VISIBLE);
    }
    private void RemoveNoAccountMsg(View v){
        v.findViewById(R.id.iv_info_no_account).setVisibility(View.GONE);
        v.findViewById(R.id.tv_info_no_account_available).setVisibility(View.GONE);
    }
    private void updateLogo(int index, String brand){
        ImageView iv = mLayoutCards.get(index).findViewById(R.id.brand_logo);
        iv.setImageResource(mLogoResIDs.get(brand));
    }
    private void updateName(int index){
        TextView tv = mLayoutCards.get(index).findViewById(R.id.account_name);
        tv.setText(mAi.get(index).name);
    }
    private void updateMail(int index){
        TextView tv = mLayoutCards.get(index).findViewById(R.id.account_mail);
        tv.setText(mAi.get(index).mail);
    }
    private void updatePhoto(int index, String brand){
        Log.d(TAG, "Update photo. Brand: " + brand);
        ImageView iv = mLayoutCards.get(index).findViewById(R.id.user_photo);
        if(brand.equals(SignInManager.BRAND_GOOGLE)){
            downloadPhotoGoogle(iv);
        }
        else if(brand.equals(SignInManager.BRAND_MS)){
            downloadPhotoMicrosoft(iv);
        }

    }
    private void downloadPhotoGoogle(ImageView iv){

        SignInGoogle google = SignInGoogle.getInstance(getContext());
        google.getPhoto(iv, new SignInManager.OnPhotoDownloaded(){

            @Override
            public void onDownloaded(Bitmap bmp, Object object) {
                //ImageView iv = mLayoutCards.get(mCardIndex).findViewById(R.id.user_photo);
                Log.d(TAG, "Google download photo done");
                ImageView iv = (ImageView)object;
                iv.setImageBitmap(bmp);

            }
        });
    }
    private void downloadPhotoMicrosoft(ImageView iv){
        mIvUserPhoto = iv;

        SignInMS ms = SignInMS.getInstance(getActivity());
        ms.getPhoto(iv, new SignInManager.OnPhotoDownloaded(){

            @Override
            public void onDownloaded(Bitmap bmp, Object object) {
                //ImageView iv = mLayoutCards.get(mCardIndex).findViewById(R.id.user_photo);
                Log.d(TAG, "Microsoft download photo done");
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        ImageView iv = (ImageView)object;
                        iv.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

//    private class UpdatePhoto extends AsyncTask<String, Void, Bitmap> {
//
//        @Override
//        protected Bitmap doInBackground(String... s) {
//            while(mBmpUserPhoto == null){
//                Log.d(TAG, "Wait for photo");
//            }
//            mIvUserPhoto.setImageBitmap(mBmpUserPhoto);
//            return null;
//        }
//    }
    /*
    Read activated accounts using account manager and save them or it to mAccountList.
     */
    private void readAllAccounts() {
        AccountManager.AccountInfo ai;
        AccountManager am = AccountManager.getInstance();

        //First of all, clean up the list
        for(int i = 0; i < mAi.size(); i++){
            mAi.remove(i);
        }

        for (int i = 0; i < mBrands.size(); i++) {
            ai = am.getAccountActivated(getContext(), mBrands.get(i));
            if (ai != null) {
                Log.d(TAG, "Activated account: " + ai.name);
//                if(ai.mail != null){Log.d(TAG, "mail: " + ai.mail);}
//                else{Log.d(TAG, "mail is empty");}
                mAi.add(ai);
            } else {
                Log.d(TAG, "No activated account for brand:" + mBrands.get(i));
            }
        }
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
