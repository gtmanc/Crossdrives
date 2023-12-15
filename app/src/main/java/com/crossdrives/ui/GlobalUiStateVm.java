package com.crossdrives.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.model.CdfsItem;

public class GlobalUiStateVm extends ViewModel {
    private MoveItemStateLd moveItemStateLd = new MoveItemStateLd();

    public class MoveItemStateLd extends LiveData<MoveItemState> {

        private MoveItemState moveState = new MoveItemState();

        public void launch(CdfsItem[] startDest){
            moveState.InProgress = true;
            moveState.startDest = startDest;
            postValue(moveState);
        }

        public MoveItemState getMoveItemState(){return moveState;}
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
        CdfsItem[] startDest;


        MoveItemState() {
            this.InProgress = false;
            this.MoveUpward = false;
            this.startDest = null;
        }

        public boolean isInProgress(){return InProgress;}

        public boolean setProgress(boolean progress){return InProgress = progress;}

    }

    public MoveItemStateLd getMoveItemStateLd(){return moveItemStateLd;}
}
