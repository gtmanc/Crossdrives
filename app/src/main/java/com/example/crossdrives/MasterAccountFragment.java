package com.example.crossdrives;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
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
    List<AccountManager.AccountInfo> mAi = new ArrayList<>();
    private View mView;
    private Fragment mFragment;
    //private List <List <ImageView>> mImageViews= new ArrayList<>(); //[0]: logo, [1]: photo
    private List<CardView> mLayoutCards = new ArrayList<>();
    private HashMap<String, Integer> mLogoResIDs= new HashMap<>();

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
        mView = view;

        String MyArg = MasterAccountFragmentArgs.fromBundle(getArguments()).getCreateAccountName();
        if(MyArg != null)
            Toast.makeText(getContext(), "Master Account Added: " + MyArg, Toast.LENGTH_LONG).show();

        readAllAccounts();

        InitializeUI(view);

        udpateCards(view);

        view.findViewById(R.id.add_account_btn).setOnClickListener(listener_account_add);
        mFragment = FragmentManager.findFragment(view);

        Toolbar toolbar = view.findViewById(R.id.master_accounts_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);

    }

    private void InitializeUI(View v){
//        ImageView iv = v.findViewById(R.id.account_list1).findViewById(R.id.brand_logo); mImageViews.get(0).add(iv);
//        iv = v.findViewById(R.id.account_list1).findViewById(R.id.user_photo); mImageViews.get(1).add(iv);
//        iv = v.findViewById(R.id.account_list2).findViewById(R.id.brand_logo); mImageViews.get(0).add(iv);
//        iv = v.findViewById(R.id.account_list2).findViewById(R.id.user_photo); mImageViews.get(1).add(iv);
        CardView iv = v.findViewById(R.id.account_list1); mLayoutCards.add(iv);
        iv = v.findViewById(R.id.account_list2); mLayoutCards.add(iv);

        mLogoResIDs.put(BrandList.get(0), new Integer(R.drawable.logo_drive_2020q4_color_2x_web_64dp));
        mLogoResIDs.put(BrandList.get(1), new Integer(R.drawable.ic_onedrive_logo));
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

        //Udate all of the cards if the account profile has at least one available
        for(int i = 0; i < mAi.size(); i++){
            setCardVisible(i);
            updateLogo(i);
            TextView t = v.findViewById(R.id.account_name);
            t.setText(mAi.get(i).name);
            t = v.findViewById(R.id.account_mail);
            t.setText(mAi.get(i).mail);
            //mAi.get(i).getPhoto(callback);
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
    private void updateLogo(int index){
        ImageView iv = mLayoutCards.get(index).findViewById(R.id.brand_logo);
        iv.setImageResource(mLogoResIDs.get(index));
    }

    /*
    Read all accounts from database and save to mAccountList.
     */
    private void readAllAccounts() {
        AccountManager.AccountInfo ai;
        AccountManager am = AccountManager.getInstance();

        for (int i = 0; i < BrandList.size(); i++) {
            ai = am.getAccountActivated(getContext(), BrandList.get(i));
            if (ai != null) {
                Log.d(TAG, "Activated Google account: " + ai.name);
                mAi.add(ai);
            } else {
                Log.d(TAG, "No activated account for brand:" + BrandList.get(i));
            }
        }
    }

    AccountManager.AccountInfo.Callback callback = new AccountManager.AccountInfo.Callback(){

        @Override
        public void onPhotoDownloaded(Bitmap bmp) {
            ImageView iv = mView.findViewById(R.id.user_photo);
            iv.setImageBitmap(bmp);
        }
    };

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
