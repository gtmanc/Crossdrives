package com.crossdrives.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;


import com.example.crossdrives.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CreateFolderDialog extends DialogFragment {
    private final String TAG = "CD.CreateFolderDialog";
    private CreateFolderDialog createFolderDialog = this;
    private TextInputLayout viewTextInputLayout;
    private TextInputEditText viewEditText;

    public static final String KEY_NAME_ENTERED = "keyNameEntered";

    // Use this instance of the interface to deliver action events.
    NoticeDialogListener listener;
    private String nameEntered;

    @Override
    // Override the Fragment.onAttach() method to instantiate the
    // NoticeDialogListener.
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface.
//        try {
//            // Instantiate the NoticeDialogListener so you can send events to
//            // the host.
//            listener = (NoticeDialogListener) context;
//        } catch (ClassCastException e) {
//            // The activity doesn't implement the interface. Throw exception.
//            throw new ClassCastException(getActivity().toString()
//                    + " must implement NoticeDialogListener");
//        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog.
        // Pass null as the parent view because it's going in the dialog layout.
        View view = inflater.inflate(R.layout.create_folder_dialog, null);
        viewEditText = view.findViewById(R.id.edit_box1_create_folder_dialog);
        builder.setView(view);

//        viewTextInputLayout = getActivity().findViewById(R.id.textInputLayout1_base_action_dialog);
//        viewEditText = getActivity().findViewById(R.id.edit_box1_base_action_dialog);

        builder.setMessage(R.string.content_create_folder_dialog)
                .setTitle(R.string.title_create_folder_dialog)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        nameEntered = viewEditText.getText().toString();
                        //listener.onDialogPositiveClick(createFolderDialog);
                        Bundle result = new Bundle();
                        result.putString(KEY_NAME_ENTERED, nameEntered);
                        getParentFragmentManager().setFragmentResult("requestKey", result);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancels the dialog.
                    }
                });
        // Create the AlertDialog object and return it.
        return builder.create();
    }

    // This is only applicable if the dialog is launched by a activity.
    // https://developer.android.com/develop/ui/views/components/dialogs#PassingEvents
    // The activity that creates an instance of this dialog fragment must
    // implement this interface to receive event callbacks. Each method passes
    // the DialogFragment in case the host needs to query it.
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(CreateFolderDialog dialog);
        public void onDialogNegativeClick(CreateFolderDialog dialog);
    }

    public String getNameEntered(){
        return nameEntered;
    }
}
