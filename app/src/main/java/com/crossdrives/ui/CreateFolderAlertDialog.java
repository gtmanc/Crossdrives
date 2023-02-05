package com.crossdrives.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.crossdrives.R;

import java.util.List;
/*
    !!!
    Give up to employ this alert dialog because we are not able to find the host fragment via the context
    received in onAttach.
    The test result shows the fragment is
    ReportFragment{c4f476c #0 androidx.lifecycle.LifecycleDispatcher.report_fragment_tag}
    which is not the host fragment.
*/
public class CreateFolderAlertDialog extends DialogFragment {
    final String TAG = "CD.BaseActionDialog";
    CreateFolderDialogListener listener;

    public interface CreateFolderDialogListener{
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Fragment fragment =
        getParentFragmentManager().findFragmentById(R.id.query_result_fragment);

        Log.d(TAG, "onViewCreated gets called.");
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (CreateFolderDialogListener) fragment;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(fragment.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Activity activity = (Activity) context;
        List<android.app.Fragment> fragments;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            fragments = activity.getFragmentManager().getFragments();
            fragments.stream().forEachOrdered((f)->{
                Log.d(TAG, "onAttach gets called. Fragment: " + f);
            });
        }

        // Verify that the host activity implements the callback interface
//        try {
//            // Instantiate the NoticeDialogListener so we can send events to the host
//            listener = (CreateFolderDialogListener) fragment;
//            Log.d(TAG, "listener:" + listener);
//        } catch (ClassCastException e) {
//            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException(context.toString()
//                    + " must implement NoticeDialogListener");
//        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();

        builder.setMessage("Test Alert Dialog")
                .setPositiveButton("Left Button", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // START THE GAME!
                        listener.onDialogPositiveClick(CreateFolderAlertDialog.this);
                    }
                })
                .setNegativeButton("Right Button", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogNegativeClick(CreateFolderAlertDialog.this);
                    }
                })
                .setView(inflater.inflate(R.layout.fab_option_alertdialog, null));
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
