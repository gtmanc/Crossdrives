package com.crossdrives.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.crossdrives.cdfs.model.CdfsItem;

public class GlobalUiStateVm extends ViewModel {
    private MoveItemStateLd moveItemStateLd = new MoveItemStateLd();

    public class MoveItemStateLd extends LiveData<MoveItemState> {
        CdfsItem startDest;
        private MoveItemState moveState = new MoveItemState(false, false);

        public void launch(CdfsItem startDest){
            startDest = startDest;
            moveState.isInProgress = true;
            moveState.atStartDest = true;
            postValue(moveState);
        }

        public MoveItemState getMoveItemState(){return moveState;}
    }

    class MoveItemState {
        boolean isInProgress;
        boolean atStartDest;

        MoveItemState(boolean isInProgress, boolean atStartDest) {
            this.isInProgress = isInProgress;
            this.atStartDest = atStartDest;
        }

        public boolean isInProgress(){return isInProgress;}

        public boolean setProgress(boolean progress){return isInProgress = progress;}

    }

    public MoveItemStateLd getMoveItemStateLd(){return moveItemStateLd;}
}
