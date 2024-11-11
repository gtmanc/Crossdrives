package com.crossdrives.ui.account;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.crossdrives.AccountManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MasterAccountVM extends ViewModel {
    final String TAG = "CD.MasterAccountVM";
    private List<AccountManager.AccountInfo> currList;

    public final static int ACCOUNT_ADDED = 0;
    public final static int ACCOUNT_REMOVED = 1;
    public final static int ACCOUNT_CHANGE_VM_CREATED = 2;
    public final static int ACCOUNT_NO_CHANGE = 3;

    public Collection<AccountManager.AccountInfo> getCurrList(){return currList;}

    public class DiffResult{
        int status;
        List<AccountManager.AccountInfo> diff;

        public int getStatus() {
            return status;
        }

        public List<AccountManager.AccountInfo> getDiff() {
            return diff;
        }
    }

    /*
            Return
            Diffed list
         */
    public DiffResult diffThenUpdateCurr() {
        AccountManager am = AccountManager.getInstance();
        List<AccountManager.AccountInfo> updatedList = new ArrayList<>(am.getAccountActivated());
        DiffResult result = new DiffResult();
        result.status = determineStatus(currList, updatedList);
        Log.d(TAG, "diff status: " + result.status);
        if (currList == null) {
            currList = new ArrayList<>();
            Log.d(TAG, "currList is null");
        }

        result.diff = diffBetweenLists(updatedList, currList);

        List<AccountManager.AccountInfo> diff2 = diffBetweenLists(updatedList, currList);
        Log.d(TAG, "diffed account: ");
        diff2.stream().forEach((ai)->{
            Log.d(TAG, "brand: " + ai.brand + ". name: " + ai.name);
        });
        currList = updatedList;
        return result;
    }

    private <T> int determineStatus(List<T> oldList, List<T> newList){
        int status = ACCOUNT_NO_CHANGE;
        int oldListSize = oldList == null? 0 : oldList.size();
        int newListSize = newList.size();

        if(oldList == null){
            status = ACCOUNT_CHANGE_VM_CREATED;
            return status;
        }

        if (oldListSize < newListSize) {
            status = ACCOUNT_ADDED;
        }else if(oldListSize > newListSize){
            status = ACCOUNT_REMOVED;
        }

        return status;
    }

    private List<AccountManager.AccountInfo> diffBetweenLists(List<AccountManager.AccountInfo> first, List<AccountManager.AccountInfo> second){
        Stream<AccountManager.AccountInfo> diff1, diff2;
        diff1 = first.stream().filter((ai1)->{
            return !(second.stream().filter((ai2)->{
                return isIdentical(ai1, ai2);
            }).count() > 0);
        });
        diff2 = second.stream().filter((ai1)->{
            return !(first.stream().filter((ai2)->{
                return isIdentical(ai1, ai2);
            }).count() > 0);
        });
        return Stream.concat(diff1, diff2).collect(Collectors.toList());
    }

    private boolean isIdentical(AccountManager.AccountInfo first, AccountManager.AccountInfo second){
        boolean isIdentical = true;
        if(!first.brand.equals(second.brand)){isIdentical = false;}
        if(!first.name.equals(second.name)){isIdentical = false;}
        if(!first.mail.equals(second.mail)){isIdentical = false;}
        //Log.d(TAG, "isIdentical: " + isIdentical);
        return isIdentical;
    }


}
