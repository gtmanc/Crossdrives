package com.crossdrives.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.model.CdfsItem;

public class GlobalUiStateVm extends ViewModel {
    private MoveItemStateLd moveItemStateLd = new MoveItemStateLd();

    public class MoveItemStateLd extends LiveData<MoveItemState> {

        private MoveItemState moveState = new MoveItemState();

        public void launch(CdfsItem[] startDest){
            moveState.isInProgress = true;
            moveState.startDest = startDest;
            postValue(moveState);
        }

        public MoveItemState getMoveItemState(){return moveState;}
    }

    class MoveItemState {
        //True: app is in move item workflow.
        //False: app has exited move item workflow but the move may has not yet done.
        boolean isInProgress;

        //Only valid when isInProgress is true.
        //TRUE: location of app is in children of start dest.
        //FALSE: location of app is in parent of start dest.
        //Used when move item workflow needs to determine whether it needs to open a new lsit screen or not.
        boolean atParentOfStartDest;
        CdfsItem[] startDest;


        MoveItemState() {
            this.isInProgress = false;
            this.atParentOfStartDest = false;
            this.startDest = null;
        }

        public boolean isInProgress(){return isInProgress;}

        public boolean setProgress(boolean progress){return isInProgress = progress;}

    }

    public MoveItemStateLd getMoveItemStateLd(){return moveItemStateLd;}
}
