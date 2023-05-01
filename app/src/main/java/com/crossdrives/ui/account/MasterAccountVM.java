package com.crossdrives.ui.account;

import android.util.Log;

import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.DiffUtil;

import com.example.crossdrives.AccountManager;
import com.example.crossdrives.GlobalConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MasterAccountVM extends ViewModel {
    final String TAG = "CD.MasterAccountVM";
    private List<AccountManager.AccountInfo> currList = new ArrayList<>();

    public void addAccountAll(Collection<AccountManager.AccountInfo> accounts){

    }

    public Collection<AccountManager.AccountInfo> getCurrList(){return currList;}
    public Collection<AccountManager.AccountInfo> calculateDiff(Collection<AccountManager.AccountInfo> accounts){
        AccountManager am = AccountManager.getInstance();
        List<AccountManager.AccountInfo> newList = new ArrayList<>(am.getAccountActivated());
        List<AccountManager.AccountInfo> diff;
        if(currList.size() == 0){diff = newList;}
        else{
            diff = newList.stream().filter((new_ai)->{
                return currList.stream().filter((curr_ai)->{
                    return !isIdentical(new_ai, curr_ai);
                }).count() > 0;
            }).collect(Collectors.toList());
        }

        Log.d(TAG, "diffed account: ");
        diff.stream().forEach((ai)->{
            Log.d(TAG, "brand: " + ai.brand + ". name: " + ai.name);
        });
        currList = diff;
        return diff;
    }

    private boolean isIdentical(AccountManager.AccountInfo first, AccountManager.AccountInfo second){
        boolean isIdentical = true;
        if(!first.brand.equals(second.brand)){isIdentical = false;}
        if(!first.name.equals(second.name)){isIdentical = false;}
        if(!first.mail.equals(second.mail)){isIdentical = false;}
        Log.d(TAG, "isIdentical: " + isIdentical);
        return isIdentical;
    }


}
