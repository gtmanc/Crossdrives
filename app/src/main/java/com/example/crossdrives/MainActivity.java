package com.example.crossdrives;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gson.internal.Pair;;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// Useful links:
// Google sign-in: https://developers.google.com/identity/sign-in/android/sign-in
// sample code in github: https://github.com/gsuitedevs/android-samples/tree/master/drive/deprecation

public class MainActivity extends AppCompatActivity{
    GoogleSignInClient mGoogleSignInClient;
    //Request code used for OnActivityResult
    private static final int RC_SIGN_IN = 0;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    private static final int RC_QUERY = 3;
    private static final int RC_DELETE_FILE = 4;
    private DriveServiceHelper mDriveServiceHelper;
    private String TAG = "CD.MainActivity";

    private String mOpenFileId;
    private ArrayList<String> mQueryFileId, mQueryFileName;
    private FileList mQueriedFileList;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        // Set the onClick listeners for the button bar.
        findViewById(R.id.sign_in_button).setOnClickListener(btn1Listener);
        findViewById(R.id.open_btn).setOnClickListener(openFilePicker);
        findViewById(R.id.query_btn).setOnClickListener(query);
        findViewById(R.id.signout_btn).setOnClickListener(signout);
        findViewById(R.id.create_btn).setOnClickListener(createFile);
        findViewById(R.id.save_btn).setOnClickListener(saveFile);
        findViewById(R.id.delete_btn).setOnClickListener(deleteFiles);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        // Note: If you need to detect changes to a user's auth state that happen outside your app,
        // such as access token or ID token revocation, or to perform cross-device sign-in,
        // you might also call GoogleSignInClient.silentSignIn when your app starts.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account != null){
            signInButton.setEnabled(false);
            //updateUI("already signed in");
            requestDriveService(account);
        }

        // Store the EditText boxes to be updated when files are opened/created/modified.
        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Log.d(TAG, "density = " + dm.density);
        Log.d(TAG, "xdpi = " + dm.xdpi);
        Log.d(TAG, "ydpi = " + dm.ydpi);
        Log.d(TAG, "densityDpi = " + dm.densityDpi);
        Log.d(TAG, "heightPixels = " + dm.heightPixels);
        Log.d(TAG, "widthPixels = " + dm.widthPixels);

    }
    private View.OnClickListener btn1Listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sign_in_button:
                    signIn();
                    break;
                // ...
            }
        }
    };
    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private View.OnClickListener openFilePicker = new View.OnClickListener() {
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
     * Queries the Drive REST API for files visible to this app and lists them in the content view.
     */
    private View.OnClickListener query = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //mQueryFileName, mQueryFileId and fileList will be updated
            //queryFile();

            Intent intent = new Intent();
            Bundle bundle = new Bundle();

            //Ready to go to the result list
            intent.setClass(MainActivity.this, QueryResultActivity.class);
            bundle.putStringArrayList("ResultList", mQueryFileName);
            intent.putExtras(bundle);
            startActivityForResult(intent, RC_QUERY);


        }
    };

    // example code for handling sign-out: https://developers.google.com/identity/sign-in/android/disconnect?hl=zh-TW
    private View.OnClickListener signout = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Task<Void> pendingResult = mGoogleSignInClient.signOut();
            pendingResult.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    updateUI("Sign out OK!");
                    Log.d(TAG, "Sign out OK!");
                }
            });
        }
    };

    /**
     * Creates a new file via the Drive REST API.
     */
    private View.OnClickListener createFile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mDriveServiceHelper != null) {
                Log.d(TAG, "Creating a file.");

                mDriveServiceHelper.createFile()
                        //.addOnSuccessListener(fileId -> readFile(fileId))
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String fileId) {
                                readFile(fileId);
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
    };

    /**
     * Saves the currently opened file created via {@link #createFile()} if one exists.
     */
    private View.OnClickListener saveFile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mDriveServiceHelper != null && mOpenFileId != null) {
                Log.d(TAG, "Saving " + mOpenFileId);

                String fileName = mFileTitleEditText.getText().toString();
                String fileContent = mDocContentEditText.getText().toString();

                mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Log.e(TAG, "Unable to save file via REST.", exception);
                            }
                        });
            }
        }
    };



    private View.OnClickListener deleteFiles = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            //Ready go to the result list
            intent.setClass(MainActivity.this, DeleteFileActivity.class);
            bundle.putStringArrayList("ResultList", mQueryFileId);
            intent.putExtras(bundle);
            startActivityForResult(intent, RC_DELETE_FILE);
        }
    };

    private void queryFile(){
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mDriveServiceHelper.queryFiles()
                    .addOnSuccessListener(new OnSuccessListener<FileList>() {
                        @Override
                        public void onSuccess(FileList fileList) {
                            List<File> f = fileList.getFiles();
                            mQueryFileId = new ArrayList<String>();
                            mQueryFileName = new ArrayList<String>();
                            mQueriedFileList = fileList;

                            //new一個Bundle物件，並將要傳遞的資料傳入
                            Log.d(TAG, "Size of filelist: " + fileList.size());
                            Log.d(TAG, "Size of list: " + f.size());

                            StringBuilder builder = new StringBuilder();
                            int i = 0;
                            for (File file : fileList.getFiles()) {
                                builder.append(file.getName()).append("\n");
                                //Log.d(TAG, "files name: " + file.getName());
                                mQueryFileId.add(file.getId());
                                mQueryFileName.add(file.getName());
                                Log.d(TAG, "id: " + mQueryFileId.get(i++));
                            }
                            String fileNames = builder.toString();

                            mFileTitleEditText.setText("File List");
                            mDocContentEditText.setText(fileNames);

                            if(f.size() == 0)
                                mDocContentEditText.setText("No files found");

                            setReadOnlyMode();



                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e(TAG, "Unable to query files.", exception);
                            //TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
                            //https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
                        }
                    });
        }
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

                            mFileTitleEditText.setText(name);
                            mDocContentEditText.setText(content);

                            MainActivity.this.setReadWriteMode(fileId);
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

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != Activity.RESULT_OK || data == null) {
            Log.d(TAG, "resultCode is not OK or intent data is null");
            return; //intent data will be null id the uer simply press BACK Button without selecting any file.
        }

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(data);
        }
        else if(requestCode == REQUEST_CODE_OPEN_DOCUMENT) {
            Log.d(TAG, "Ready to open document!");
            updateUI("Ready to open document!");
            Uri uri = data.getData();
            if (uri != null){
                openFileFromFilePicker(uri);
            }
            else{
                Log.d(TAG, "URL is null!");
            }
        }
        else if(requestCode == RC_QUERY)
        {
            //Do nothing for showing list of queried file
            Log.d(TAG, "Request code is RC_QUERY");
        }
        else if (requestCode == RC_DELETE_FILE)
        {
            delete(data.getStringExtra("SelectedFiles"));
        }
    }

    private void handleSignInResult(Intent result) {

        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>(){
                    @Override
                    public void onSuccess(GoogleSignInAccount googleAccount)
                    {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                        //updateUI("Signed in as " + googleAccount.getEmail());

                        GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                        credential.setSelectedAccount(googleAccount.getAccount());
                        Drive googleDriveService =
                           new Drive.Builder(
                                  AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                   credential)
                                    .setApplicationName("Drive API Migration")
                                    .build();
                        updateUI("Ready to use Google drive!");
                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    //mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                        mDriveServiceHelper =  DriveServiceHelper.getInstance(googleDriveService);

                    }
                })
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleAccount) {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                    }
                });
    }

    private void requestDriveService(GoogleSignInAccount googleAccount){

        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        getApplicationContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());
        Drive googleDriveService =
                new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Drive API Migration")
                        .build();
        updateUI("Signed in already. Ready to use Google drive!");
        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
        // Its instantiation is required before handling any onClick actions.
        //mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        mDriveServiceHelper = DriveServiceHelper.getInstance(googleDriveService);
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(e.getStatusCode());
        }
    }

    /**
     * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
     * initiated by .
     */
    private void openFileFromFilePicker(Uri uri) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening " + uri.getPath());
            //some UI test
            //updateUI("Opening " + uri.getPath()); -> works
            mFileTitleEditText.setText(uri.getPath()); // -> doenst work
            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>() {
                        @Override
                        public void onSuccess(Pair<String, String> nameAndContent) {
                            String name = nameAndContent.first;
                            String content = nameAndContent.second;

                            mFileTitleEditText.setText(name);
                            mDocContentEditText.setText(content);

                            // Files opened through SAF cannot be modified.
                            setReadOnlyMode();
                            Log.d(TAG, "File name is " + name);
                            //Log.d(TAG, "Content: " + content);
                            updateUI("Open doc works!");
                            //updateUI("Open doc " + name + " works!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            updateUI("Open file using SAF failed!");
                            Log.e(TAG, "Unable to open file from picker.", exception);
                        }
                    });
        }
        else{
            Log.d(TAG, "mDriveServiceHelper is null!");
        }
    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        mFileTitleEditText.setEnabled(true);
        mDocContentEditText.setEnabled(true);
        mOpenFileId = fileId;
    }
    private void updateUI(GoogleSignInAccount account)
    {
        Log.w(TAG, "UpdateUI");
        TextView info = (TextView)findViewById(R.id.info);
        info.setText("Signed in as: " + account.getEmail());
    }

    private void updateUI(int ec)
    {
        Log.w(TAG, "UpdateUI" + ec);
        TextView info = (TextView)findViewById(R.id.info);
        if(info == null)
            Log.w(TAG, "info is null");
        info.setText(Integer.toString(ec));
    }

    private void updateUI(String s)
    {
        TextView info = (TextView)findViewById(R.id.info);
        if(info == null)
            Log.w(TAG, "info is null");
        //else
            //Log.d(TAG, "Update UI: " + s);
        info.setText(s);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
