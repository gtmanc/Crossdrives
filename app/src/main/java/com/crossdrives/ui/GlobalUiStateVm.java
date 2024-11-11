package com.crossdrives.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.model.CdfsItem;

public class GlobalUiStateVm extends ViewModel {
    private MoveItemStateLd moveItemStateLd = new MoveItemStateLd();
    private RenameStateLd renameStateLd = new RenameStateLd();

    public class MoveItemStateLd extends LiveData<MoveItemState> {

        private MoveItemState moveState = new MoveItemState();

        public void launch(CdfsItem[] startDest, int srcDestId){
            moveState.InProgress = true;
            moveState.startDest = startDest;
            moveState.srcDestId = srcDestId;
            moveState.setBackstackEntryCount(0);
            postValue(moveState);
        }

        public MoveItemState getMoveItemState(){return moveState;}
    }

    public class RenameStateLd extends LiveData<RenameState> {

        private RenameState state = new RenameState();

        public void launch(Object o){
            state.setObject(o);
            postValue(state);
        }

        public RenameState getRenameState(){return state;}
    }

    class MoveItemState {
        //True: app is in move item workflow.
        //False: app has exited move item workflow but the move may has not yet done.
        boolean InProgress;

        //Only valid when isInProgress is true.
        //TRUE: location of app is in children of start dest.
        //FALSE: location of app is in parent of start dest.
        //Used when move item workflow needs to determine whether it needs to open a new lsit screen or not.
        boolean MoveUpward;
        // This is a workaround
        // Ideally we should use FragmentManager.getBackStackEntryCount instead of creating a parameter for
        // maintaining the count of back stack entry. However, I encountered an issue that the count is
        // always 0 when I call FragmentManager.getBackStackEntryCount. Not yet figure out why it happens.
        int backstackEntryCount;
        CdfsItem[] startDest;

        int srcDestId;

        MoveItemState() {
            this.InProgress = false;
            this.MoveUpward = false;
            this.startDest = null;
        }

        public boolean isInProgress(){return InProgress;}

        public boolean setProgress(boolean progress){return InProgress = progress;}

        public void setStartDest(CdfsItem[] startDest) {
            this.startDest = startDest;
        }

        public CdfsItem[] getStartDest() {
            return startDest;
        }
        public int getBackstackEntryCount() {
            return backstackEntryCount;
        }
        public void setBackstackEntryCount(int backstackEntryCount) {
            this.backstackEntryCount = backstackEntryCount;
        }
        public int increaseBackstackEntryCount(){
            this.backstackEntryCount++;
            return this.backstackEntryCount;
        }
        public int decreaseBackstackEntryCount(){
            this.backstackEntryCount--;
            return this.backstackEntryCount;
        }
    }

    class RenameState{
        Object o;

        public Object get() {
            return o;
        }

        public void setObject(Object o) {
            this.o = o;
        }
    }

    public MoveItemStateLd getMoveItemStateLd(){return moveItemStateLd;}

    public RenameStateLd getRenameStateLd(){return renameStateLd;}
}
