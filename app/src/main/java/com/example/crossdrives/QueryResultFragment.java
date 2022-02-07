package com.example.crossdrives;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.SearchView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crossdrives.cdfs.CDFS;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueryResultFragment extends Fragment implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener,
		DrawerLayout.DrawerListener{
	private String TAG = "CD.QueryResultFragment";
	DrawerLayout mDrawer = null;
	NavigationView mNavigationView;

	private DriveServiceHelper mDriveServiceHelper;
	private RecyclerView.LayoutManager mLayoutManager;
	private ArrayList<SerachResultItemModel> mItems;
	private RecyclerView mRecyclerView = null;
	private View mProgressBar = null;
	private QueryFileAdapter mAdapter;
	private Toolbar mToolbar = null;

	final String STATE_NORMAL = "state_normal";
	final String STATE_ITEM_SELECTION = "state_selection";
	private String mState = STATE_NORMAL;
	private int mSelectedItemCount = 0;
	private int mCountPressDrawerHeader = 0;

	private ActionMode mActionMode = null;
	/*
	Next page handler. Use this handler to get file list of next page. It is available in response of
	previous file list request
	here we keep it as abstract because it varies depending on the drives
	 */
	private Object mNextPage;

	/*

	 */
	private final String QSTATE_READY = "query ready";	//Query has not yet started
	private final String QSTATE_INPROGRESS = "query ongoing";	//A query is ongoing
	private final String QSTATE_EOL = "query EOL"; //Query reach the end of list
	private String mQSTATE;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		View v = inflater.inflate(R.layout.query_result_fragment, container, false);

		//mDriveServiceHelper = DriveServiceHelper.getInstance();

		return v;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);

		NavController navController = Navigation.findNavController(view);
		DrawerLayout drawerLayout = view.findViewById(R.id.layout_query_result);
		mDrawer = drawerLayout;
		drawerLayout.addDrawerListener(this);

		AppBarConfiguration appBarConfiguration =
				new AppBarConfiguration.Builder(navController.getGraph()).setOpenableLayout(drawerLayout).build();

		mToolbar = view.findViewById(R.id.qr_toolbar);

		mProgressBar = view.findViewById(R.id.progressBar);

		//Note: drawer doenst work if this line of code is added after setupWithNavController
		((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
		Log.d(TAG, "Set toolbar done");

		NavigationUI.setupWithNavController(
				mToolbar, navController, appBarConfiguration);

		mNavigationView = view.findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(this);
		mNavigationView.getMenu().findItem(R.id.nav_item_hidden).setVisible(false);
		View hv = mNavigationView.getHeaderView(0);
		hv.setOnClickListener(this);
		//always register the callback because it is removed in onPause
		requireActivity().getOnBackPressedDispatcher().addCallback(callback);

		mRecyclerView = view.findViewById(R.id.recycler_view);
		mLayoutManager = new LinearLayoutManager(getContext());
		//It seems to be ok we create a new layout manager ans set to the recyclarview.
		//It is observed each time null is got if view.getLayoutManager is called
		mRecyclerView.setLayoutManager(mLayoutManager);

		//be sure to register the listener after layout manager is set to recyclerview
		mRecyclerView.addOnScrollListener(onScrollListener);

		mProgressBar.setVisibility(View.VISIBLE);

		initialQuery();
		queryFile(view);
	}

	/*
	 *  First time query
	 */
	private void queryFile(final View v){

		//if (mDriveServiceHelper != null) {
			Log.d(TAG, "Querying for files.");

			//mProgressBar.setVisibility(View.VISIBLE);

//			mDriveServiceHelper.resetQuery();
			setQStateInprogress();
			CDFS.getCDFSService(getActivity()).getService().list(mNextPage)
			//mDriveServiceHelper.queryFiles()
					.addOnSuccessListener(new OnSuccessListener<FileList>() {
						@Override
						public void onSuccess(FileList fileList) {
							List<File> f = fileList.getFiles();
							//ListView listview = (ListView) findViewById(R.id.listview_query);


							mItems = new ArrayList<>();
							Log.i(TAG, "Number of files: " + f.size());
							for (File file : fileList.getFiles()) {
//                                if(file.getModifiedTime() == null){
//                                    Log.w(TAG, "Modified dateTime is null");
//                                }

								//Log.d(TAG, "files name: " + file.getName());
								mItems.add(new SerachResultItemModel(false, file.getName(), file.getId(), file.getModifiedTime()));
							}

							mAdapter = new QueryFileAdapter(mItems, getContext());
							mAdapter.setOnItemClickListener(itemClickListener);
							mRecyclerView.setAdapter(mAdapter);

							mNextPage = fileList.getNextPageToken();
							if(mNextPage == null){
								Log.d(TAG, "Next page handler is null!");
								CloseQuery();
							}
							mProgressBar.setVisibility(View.INVISIBLE);
						}

					})
					.addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception exception) {
							//mProgressBar.setVisibility(View.GONE);
							Log.e(TAG, "Unable to query files.", exception);
							//TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
							//https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
							mProgressBar.setVisibility(View.INVISIBLE);
						}
					});
		//}
	}

	private void queryFileContinue(){

		//We are reaching the end of list. Stop query.
		//We are okay because no filter is applied.
		if (getState() == QSTATE_EOL) {
			CloseQuery();
			Log.d(TAG, "End of List. Exit directly");
			return;
		}

		Log.d(TAG, "Querying for files continue.");

		//mProgressBar.setVisibility(View.VISIBLE);

		//Insert a null item so that the adapter knows that progress bar needs to be shown to the user
		mItems.add(null);
		Log.d(TAG, "Notify inserted");
		mAdapter.notifyItemInserted(mItems.size() - 1);

		//mDriveServiceHelper.queryFiles()
		CDFS.getCDFSService(getActivity()).getService().list(mNextPage)
					.addOnSuccessListener(new OnSuccessListener<FileList>() {
						@Override
						public void onSuccess(FileList fileList) {
							List<File> f = fileList.getFiles();
							int i = 0;
							//now we are done with the query. take out the progress bar from the list
							Log.i(TAG, "Notify removed");
							mItems.remove(mItems.size() - 1);
							mAdapter.notifyItemRemoved(mItems.size());

							Log.i(TAG, "Number of files fetched: " + f.size());

							for (File file : fileList.getFiles()) {
//                                if(file.getModifiedTime() == null){
//                                    Log.w(TAG, "Modified dateTime is null");
//                                }
								//ItemModelBase item = mItems.get(i);
								mItems.add(new SerachResultItemModel(false, file.getName(), file.getId(), file.getModifiedTime()));
								//item.setName(file.getName());
								i++;
							}

							//now update adapter
							//mAdapter.updateRecords(mItems);
							Log.d(TAG, "Notify data set change");
							//TODO: to clarify why the newly loaded items are not updated to screen if we dont do any further scroll.
							// i.e. enter the recycler view from previous screen and only few items are initially loaded
							mAdapter.notifyDataSetChanged();

							mNextPage = fileList.getNextPageToken();
							if(mNextPage == null){
								Log.d(TAG, "Next page handler is null!");
								CloseQuery();
							}
						}
					})
					.addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception exception) {
							//mProgressBar.setVisibility(View.GONE);
							Log.e(TAG, "Unable to query files.", exception);
							//TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
							//https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
						}
					});
		//}
	}

	private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
			super.onScrollStateChanged(recyclerView, newState);
		}

		@Override
		public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

			//Log.d(TAG, "Onscroll mItems.Size:" + mItems.size());
			//fetch next page if last item is already shown to the user
			if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == mItems.size() - 1) {
				//bottom of list!
				queryFileContinue();
			}
		}
	};

	/*
		Query related methods
	*/
	/*
        Initialization of query
     */
	private void initialQuery(){
		Log.i(TAG, "initialQuery...");

		mNextPage = null;	//null to get first page of file list
		mQSTATE = QSTATE_READY;
	}

	private void setQStateInprogress(){
		mQSTATE = QSTATE_INPROGRESS;
	}

	private String getState(){
		return mQSTATE;
	}

	void CloseQuery(){
		mQSTATE = QSTATE_EOL;
	}

	/*
    Two states :
    1. Normal
    2. Item selection
    Behavior:
    click
    1. If we are not in item selection state, go to the detail of the item
    2. If we are in item selection state (by long press any of the items), then two possibilities
    2.1 click on the selected item, exit the selection state
    2.2 click on the others, select the item. (change the checkbox in the item to "checked")
    long press
    1. If we are not in item selection state, switch to item selection state
    2. If we are already in item selection state, then two possibilities:
    2.1 Long press on the selected item, exit exit the selection state
    2.2 Long press on the others, select the item. (change the checkbox in the item to "checked")
     */
	private QueryFileAdapter.OnItemClickListener itemClickListener = new QueryFileAdapter.OnItemClickListener() {
		@Override
		public void onItemClick(View view, int position){
			InputStream stream;
			Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Pressed!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Short press item:" + position);
			Log.d(TAG, "Count of selected:" + mSelectedItemCount);
			SerachResultItemModel item = mItems.get(position);

			if (view == view.findViewById(R.id.iv_more_vert)) {
				Log.d(TAG, "More_vert pressed!");
			}

			if (mState == STATE_NORMAL) {
				Log.d(TAG, "Start to download file: " + item.mName);
				//Log.d(TAG, "File ID: " + item.mId);
				//TODO: open detail of file
				CDFS.getCDFSService(getActivity()).getService().download(item.getID()).addOnSuccessListener(new OnSuccessListener<OutputStream>() {
					@Override
					public void onSuccess(OutputStream stream) {
						Log.d(TAG, "Content of file downloaded: " + stream.toString());
						Toast.makeText(getContext(), "stream.toString()", Toast.LENGTH_LONG).show();
						try {
							stream.close();
						} catch (IOException e) {
							Log.w(TAG, "Cant close output stream!");
							Toast.makeText(getContext(), "Cant close output stream!", Toast.LENGTH_LONG).show();
						}
					}
				}).addOnFailureListener(new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						Log.w(TAG, "file download failed: " + e.toString());
						Toast.makeText(getContext(), "file download failed" + e.toString(), Toast.LENGTH_LONG).show();
					}
				});
			} else {
				if (item.isSelected()) {
                    /*
                    click on the selected item, again exit item selection state
                    */
					setItemChecked(item, position, false);
					if (mSelectedItemCount == 0) {
						mAdapter.setOverflowIconVisible(true);
						mState = STATE_NORMAL;
						exitActionMode(view);
					}
				} else {
                    /*
                    click on the others, select the item. (change the checkbox in the item to "checked")
                    */
					setItemChecked(item, position, true);
				}
				updateActionModeTitle();
			}

			//now update adapter
			mItems.set(position, item);
			//mAdapter.notifyItemChanged(position);
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onItemLongClick(View view, int position) {

			Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Long Pressed!", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Long press item:" + position);
			Log.i(TAG, "Count of selected:" + mSelectedItemCount);

			SerachResultItemModel item = mItems.get(position);

			if(mState == STATE_NORMAL) {
                /*
                Switch to item selection state
                 */
				setItemChecked(item, position, true);
				mAdapter.setOverflowIconVisible(false);
				mState = STATE_ITEM_SELECTION;
				switchActionMode(view);
				//The action mode title update will be done in the actiom mode callback instead of here because mActioMode may be null at the moment.
			}else {
				if(item.isSelected()) {
                    /*
                    Long press on the selected item, exit item selection state
                    */
					setItemChecked(item, position, false);
					if(mSelectedItemCount == 0) {
						mAdapter.setOverflowIconVisible(true);
						mState = STATE_NORMAL;
						exitActionMode(view);
					}
				}else {
                    /*
                    Long press on the others, select the item. (change the checkbox in the item to "checked")
                    */
					setItemChecked(item, position, true);

				}
				updateActionModeTitle();
			}

			//now update adapter
			mItems.set(position, item);
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onImageItemClick(View view, int position) {
			Log.i(TAG, "onImageItemClick:" + position);
			PopupMenu popup = new PopupMenu(getContext(), view);
			MenuInflater inflater = popup.getMenuInflater();
			inflater.inflate(R.menu.menu_context, popup.getMenu());
			popup.show();
		}
	};
	private void setItemChecked(SerachResultItemModel item, int position, boolean checked){

		if(checked == false && mSelectedItemCount <= 0) {
			Log.i(TAG, "No item should be unchecked!!");
			//return;
		}

		item.setSelected(checked);
		if(checked == true)
			mSelectedItemCount++;
		else
			mSelectedItemCount--;
	}

	//de-selected all items. This method maintains the selected count (mSelectedItemCount).
	//The method first check if a item has been selected. If yes, deselected it.
	private void deselectAllItems(){
		int i = 0;
		for(Iterator iter = mItems.iterator();iter.hasNext();) {
			SerachResultItemModel item = (SerachResultItemModel) iter.next();
			if(item.isSelected) {
				item.setSelected(false);
				mSelectedItemCount--;
			}
		}

	}
	@Override
	public void onClick(View v) {
		Log.d(TAG, "header is clicked");
		mCountPressDrawerHeader++;

		NavDirections a = QueryResultFragmentDirections.navigateToSystemTest();
		NavHostFragment.findNavController(this).navigate(a);

	}

	@Override
	public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
//        Log.i(TAG, "onDrawerSlide: "+ slideOffset);
//        MenuItem item = mNavigationView.getCheckedItem();
//        if (item != null){
//            Log.d(TAG, "set item unchecked: "+ item.getItemId());
//            item.setChecked(false);
//        }

	}


	@Override
	public void onDrawerOpened(@NonNull View drawerView) {

	}

	@Override
	public void onDrawerClosed(@NonNull View drawerView) {
		MenuItem item = mNavigationView.getCheckedItem();
		Log.d(TAG, "navigate to: ");
		if(item != null){
			if(item.getItemId() == R.id.drawer_menu_item_master_account) {
				Log.d(TAG, "Master account fragment");
//                QueryResultFragmentDirections.NavigateToMasterAccount action =
//                        QueryResultFragmentDirections.navigateToMasterAccount();
//                action.setMyArg(100);
				NavDirections a = QueryResultFragmentDirections.navigateToMasterAccount(null);
				NavHostFragment.findNavController(this).navigate(a);
			}
			else if(item.getItemId() == R.id.drawer_menu_item_two){
				Log.d(TAG, "delete file fragment");
				NavDirections a = QueryResultFragmentDirections.navigateToDeleteFile();
				NavHostFragment.findNavController(this).navigate(a);
			}
			else{
				Log.d(TAG, "Oops, unknown ID");
			}
		}
		else{
			Log.w(TAG, "drawer checked item is null");
		}
		//It's unclear how to clear(reset) a checked item once it is checked.
		//A workaround is used: set to the hidden item so that we can avoid the unexpected transition
		mNavigationView.setCheckedItem(R.id.nav_item_hidden);
	}

	@Override
	public void onDrawerStateChanged(int newState) {

	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if(item.isChecked())
			Log.d(TAG, "checked!");

		//close drawer right here. Otherwise, the drawer is still there if screen is switched back from next one
		mDrawer.closeDrawers();

		//The screen transition will take place in callback onDrawerClosed. This is because we have to ensure that the
		//drawer is closed exactly before screen proceed to next one
		if (id == R.id.drawer_menu_item_master_account) {
			Log.d(TAG, "Master account selected!");
		}else if(id == R.id.drawer_menu_item_two){
			Log.d(TAG, "nav_item_two selected!");
		}else if(id == R.id.nav_item_hidden){
			Log.d(TAG, "nav_item_three selected!");
		}else{
			Log.d(TAG, "Unknown selected!");
		}
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		//remove the callback otherwise the callback will be called in next fragment. This is fine
		//because the callback is always registered in onViewCreated.
		callback.remove();
	}

	//Back key handling
	OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
		@Override
		public void handleOnBackPressed() {
			//Go back to launcher
			Intent homeIntent = new Intent(Intent.ACTION_MAIN);
			homeIntent.addCategory( Intent.CATEGORY_HOME );
			homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
		}
	};

	private void switchActionMode(View view){
		Log.d(TAG, "switchActionMode...");
		if (mActionMode == null) {
			// Start the CAB using the ActionMode.Callback defined above
			mActionMode = getActivity().startActionMode(actionModeCallback);
//            if(mActionMode.isTitleOptional())
//                Log.d(TAG, "Action mode title is optional!");
//            mActionMode.setTitleOptionalHint(false);
			updateActionModeTitle();
			view.setSelected(true);
		}
	}

	private void exitActionMode(View view){
		if (mActionMode != null) {
			// Start the CAB using the ActionMode.Callback defined above
			mActionMode.finish();
			view.setSelected(false);
		}
	}

	private void updateActionModeTitle(){
		if(mActionMode!=null)
			mActionMode.setTitle(Integer.toString(mSelectedItemCount)+" Selected");
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		Log.d(TAG, "onCreateOptionsMenu...");
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.menu_option, menu);

		// Associate searchable configuration with the SearchView
		SearchManager searchManager =
				(SearchManager) getActivity().getSystemService(getContext().SEARCH_SERVICE);
		SearchView searchView =
				(SearchView) menu.findItem(R.id.search).getActionView();
		searchView.setSearchableInfo(
				searchManager.getSearchableInfo(getActivity().getComponentName()));
		//searchView.setSubmitButtonEnabled(true);
	}

	private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Log.d(TAG, "onCreateActionMode");
			mToolbar.setVisibility(View.GONE);
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.menu_context, menu);
			//This is a workaround: although the property AsAsAction is set to "never" for those
			// items in menu. But it doesnt work.
			menu.findItem(R.id.miDetail).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			menu.findItem(R.id.miCopy).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			MenuItem copy = menu.findItem(R.id.miDetail).setOnMenuItemClickListener(OnMenuItemClickListener);

			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return false;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			Log.d(TAG, "onDestroyActionMode");
			mActionMode = null;
			deselectAllItems();
			mState = STATE_NORMAL;
			mToolbar.setVisibility(View.VISIBLE);
			mAdapter.setOverflowIconVisible(true);
			mAdapter.notifyDataSetChanged();
		}
	};

	private MenuItem.OnMenuItemClickListener OnMenuItemClickListener = new MenuItem.OnMenuItemClickListener(){
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			//YourActivity.this.someFunctionInYourActivity();
			Log.d(TAG, "Menu item action pressed!!");
			return true;
		}
	};
}