package com.example.crossdrives;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.DriveScopes;


import org.jetbrains.annotations.NotNull;

public class SystemTestFragment extends Fragment {
    private String TAG = "CD.SystemTestFragment";
    GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 1; /* unique request id */
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private static final int RC_QUERY = 3;
    private static final int RC_DELETE_FILE = 4;

    final String SUBJECT_ACCOUNT = "Account:";
    TextView mSubjectAccount = null;


    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView enter...");
        View v = inflater.inflate(R.layout.system_test_fragment, container, false);

        mDriveServiceHelper = DriveServiceHelper.getInstance();

        return v;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        view.findViewById(R.id.system_test_button_signout).setOnClickListener(OnClickSingOut);
        // Set the dimensions of the sign-in button.
        //SignInButton signInButton = findViewById(R.id.sign_in_button);
        //signInButton.setSize(SignInButton.SIZE_WIDE);
        view.findViewById(R.id.system_test_button_create_file).setOnClickListener(OnClickCreateFile);
        view.findViewById(R.id.system_test_button_explorer).setOnClickListener(OnClickExplorer);

        mSubjectAccount = view.findViewById(R.id.system_test_subject_account);
        updateSigninStatus();

        mDriveServiceHelper =  DriveServiceHelper.getInstance();
        if(mDriveServiceHelper == null)
            Log.w(TAG,"mDriveServiceHelper is null!");
    }

    // example code for handling sign-out: https://developers.google.com/identity/sign-in/android/disconnect?hl=zh-TW
    // Google offcial: https://developers.google.com/identity/sign-in/android/disconnect
    private View.OnClickListener OnClickSingOut = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Log.d(TAG, "Sign out button is clicked");
            Task<Void> pendingResult = mGoogleSignInClient.signOut();
            pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Log.d(TAG, "Sign out OK!");
                    revokeAccess();
                    updateSigninStatus();
                }
            });
        }
    };

    private View.OnClickListener OnClickCreateFile = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Create File button is clicked");
            Intent intent = new Intent(getContext(), SysTestEnterFileDialog.class);

            mStartForResult.launch(intent);
        }
    };

    private void revokeAccess() {
        Task<Void> pendingResult = mGoogleSignInClient.revokeAccess();
        pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "revoke OK!");
                    }
                });
    }

    private void updateSigninStatus(){
        //Google
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        // Note: If you need to detect changes to a user's auth state that happen outside your app,
        // such as access token or ID token revocation, or to perform cross-device sign-in,
        // you might also call GoogleSignInClient.silentSignIn when your app starts.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if(account != null) {
            mSubjectAccount.setText(SUBJECT_ACCOUNT + " signed in as " + account.getEmail());
        }
        else{
            mSubjectAccount.setText(SUBJECT_ACCOUNT + " not yet sign in ");
        }
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private View.OnClickListener OnClickExplorer = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if (mDriveServiceHelper != null) {
                Log.d(TAG, "Opening file picker.");

                Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

                // The result of the SAF Intent is handled in onActivityResult.
                startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
            }
        }
    };

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private View.OnClickListener OnClickDelete = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if (mDriveServiceHelper != null) {
                Log.d(TAG, "Opening file picker.");
                //delete();
            }
        }
    };

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
        String mFileName = null;
        String mFileContent = null;
        String mOpenFID = null;
        @Override
        public void onActivityResult(ActivityResult result) {
            Log.d(TAG, "onActivityResult is called. Result code:" + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        mFileName = intent.getStringExtra("Name");
                        mFileContent = intent.getStringExtra("Text");
                        Log.d(TAG, "Input Name: " + mFileName);
                        Log.d(TAG, "Input Text: " + mFileContent);
                        // Handle the Intent
                        mDriveServiceHelper.createFile()
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String fileId) {
                                        Log.d(TAG, "Create file OK");
                                        mOpenFID = fileId;
                                        mDriveServiceHelper.saveFile(mOpenFID, mFileName, mFileContent)
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception exception) {
                                                        Log.e(TAG, "Unable to save file.", exception);
                                                    }
                                                });
                                    }

                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Log.e(TAG, "Couldn't create file.", exception);
                                    }
                                });
                    }
                }
            });

    private void showDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        Log.d(TAG, "density = " + dm.density);
        Log.d(TAG, "xdpi = " + dm.xdpi);
        Log.d(TAG, "ydpi = " + dm.ydpi);
        Log.d(TAG, "densityDpi = " + dm.densityDpi);
        Log.d(TAG, "heightPixels = " + dm.heightPixels);
        Log.d(TAG, "widthPixels = " + dm.widthPixels);
    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    private void readFile(final String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>() {
                        @Override
                        public void onSuccess(Pair<String, String> nameAndContent) {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;
                            //Do whatever you like
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Couldn't read file.", exception);
                        }
                    });
        }
    }

    void delete(String Id){
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "delete " + Id);

            mDriveServiceHelper.deleteFile(Id)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Unable to delete file via REST.", exception);
                        }
                    });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult");

        if(resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "resultCode is not OK: [" + resultCode + "]");
        }

        if(data == null) {
            Log.d(TAG, "Intent data is null");
            return; //intent data will be null id the uer simply press BACK Button without selecting any file.
        }

        if(data.getExtras() == null) {
            Log.d(TAG, "Intent.getExtras is null");
            return; //intent data will be null id the uer simply press BACK Button without selecting any file.
        }

        if(requestCode == REQUEST_CODE_OPEN_DOCUMENT) {
            Log.d(TAG, "Ready to open document!");
            Uri uri = data.getData();
            if (uri != null){
                openFileFromFilePicker(uri);
            }
            else{
                Log.d(TAG, "URL is null!");
            }
        }

        else if (requestCode == RC_DELETE_FILE)
        {
            delete(data.getStringExtra("SelectedFiles"));
        }
    }

    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by .
     */
    private void openFileFromFilePicker(Uri uri) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening " + uri.getPath());
            mDriveServiceHelper.openFileUsingStorageAccessFramework(getActivity().getContentResolver(), uri)
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>() {
                        @Override
                        public void onSuccess(Pair<String, String> nameAndContent) {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Unable to open file from picker.", exception);
                        }
                    });
        }
        else{
            Log.d(TAG, "mDriveServiceHelper is null!");
        }
    }
}
