package com.crossdrives.ui;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crossdrives.cdfs.delete.IDeleteProgressListener;
import com.crossdrives.cdfs.download.IDownloadProgressListener;
import com.crossdrives.cdfs.exception.PermissionException;
import com.crossdrives.ui.document.Open;
import com.crossdrives.ui.document.OpenTree;
import com.crossdrives.ui.helper.CreateFolderDialogBuilder;
import com.crossdrives.ui.helper.CreateFolderDialogResultResolver;
import com.crossdrives.ui.listener.ProgressUpdater;
import com.crossdrives.ui.listener.ResultUpdater;
import com.crossdrives.ui.notification.Notification;
import com.crossdrives.cdfs.CDFS;
import com.crossdrives.cdfs.Service;
import com.crossdrives.cdfs.exception.GeneralServiceException;
import com.crossdrives.cdfs.exception.MissingDriveClientException;
import com.crossdrives.cdfs.upload.IUploadProgressListener;
import com.crossdrives.msgraph.SnippetApp;
import com.example.crossdrives.DriveServiceHelper;
import com.example.crossdrives.R;
import com.example.crossdrives.SerachResultItemModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.api.services.drive.model.File;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QueryResultFragment extends Fragment implements DrawerLayout.DrawerListener, CreateFolderAlertDialog.CreateFolderDialogListener{
	private String TAG = "CD.QueryResultFragment";
	DrawerLayout mDrawer = null;
	NavigationView mNavigationView, mBottomNavigationView;

	private DriveServiceHelper mDriveServiceHelper;
	private RecyclerView.LayoutManager mLayoutManager;
	//private List<SerachResultItemModel> mItems;	//items used to render UI
	private RecyclerView mRecyclerView = null;
	private View mProgressBar = null;
	//private QueryFileAdapter mAdapter;
	private RootItemsAdapter mAdapter;

	private Toolbar mToolbar, mBottomAppBar;
	private View mView = null;
	private BottomSheetBehavior bottomSheetBehavior;

	final String STATE_NORMAL = "state_normal";
	final String STATE_ITEM_SELECTION = "state_selection";
	private String mState = STATE_NORMAL;
	private int mSelectedItemCount = 0;

	private int mCountPressDrawerHeader = 0;

	private ActionMode mActionMode = null;

	private Activity mActivity;


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
	//private Querier mQuerier = new Querier();
	private boolean isQueryOngoing = false;

	OpenTree treeOpener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setHasOptionsMenu(true);

		treeOpener = new ViewModelProvider(getActivity()).get(OpenTree.class);
//		treeOpener.getItems().observe(this, listChangeObserver);
		Log.d(TAG, "TreeOpen object: " + treeOpener);

		mAdapter = new RootItemsAdapter(getContext());
		treeOpener.getItems().observe(this, list -> mAdapter.submitList(list));
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");

		View v = inflater.inflate(R.layout.query_result_fragment, container, false);
		return v;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);

		mView = view;
		mActivity = getActivity();
		NavController navController = Navigation.findNavController(view);
		DrawerLayout drawerLayout = view.findViewById(R.id.layout_query_result);
		mDrawer = drawerLayout;
		drawerLayout.addDrawerListener(this);
		FloatingActionButton fab = view.findViewById(R.id.fab);

		AppBarConfiguration appBarConfiguration =
				new AppBarConfiguration.Builder(navController.getGraph()).setOpenableLayout(drawerLayout).build();

		mToolbar = view.findViewById(R.id.qr_toolbar);
		mBottomAppBar = view.findViewById(R.id.bottomAppBar);
		mProgressBar = view.findViewById(R.id.progressBar);

		//Note: drawer doesn't work if this line of code is added after setupWithNavController
		((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
		Log.d(TAG, "Set toolbar done");

		NavigationUI.setupWithNavController(
				mToolbar, navController, appBarConfiguration);

		mNavigationView = view.findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(OnNavigationItemSelectedListener);
		mNavigationView.getMenu().findItem(R.id.nav_item_hidden).setVisible(false);
		View hv = mNavigationView.getHeaderView(0);
		hv.setOnClickListener(onHeaderClick);
		fab.setOnClickListener(onFabClick);

		mBottomNavigationView = view.findViewById(R.id.bottomNavigationView);
		mBottomNavigationView.setNavigationItemSelectedListener(OnBottomNavItemSelectedListener);
		//mBottomAppBar.setNavigationOnClickListener(); //e.g. the drawer icon. We never use so far
		mBottomAppBar.setOnMenuItemClickListener(onBottomAppBarMenuItemClickListener);

		View bottomSheet = view.findViewById(R.id.bottomNavigationView);
		bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
		bottomSheetBehavior.setHideable(true);//this one has been set to true in layout
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		//always register the callback because it is removed in onPause
		requireActivity().getOnBackPressedDispatcher().addCallback(callback);

		mRecyclerView = view.findViewById(R.id.recycler_view);
		mLayoutManager = new LinearLayoutManager(getContext());
		//It seems to be ok we create a new layout manager ans set to the recyclarview.
		//It is observed each time null is got if view.getLayoutManager is called
		//Reason why we need to set layout manager: https://stackoverflow.com/questions/50171647/recyclerview-setlayoutmanager
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setAdapter(mAdapter);
		mAdapter.setNotifier(AdapterNotifier);

		//be sure to register the listener after layout manager is set to recyclerview
		mRecyclerView.addOnScrollListener(onScrollListener);

//		mItems = new ArrayList<>();
//		mAdapter = new QueryFileAdapter(mItems, getContext());
//		mAdapter.setOnItemClickListener(itemClickListener);
//		mRecyclerView.setAdapter(mAdapter);

		//view.findViewById(R.id.scrim).setOnClickListener(onScrimClick);

		mProgressBar.setVisibility(View.VISIBLE);

//		initialQuery();
//		queryFile();
	}

	/*
	 *  First time query
	 */
	private void queryFile(){

		//if (mDriveServiceHelper != null) {
			Log.d(TAG, "Querying for files.");

			//mProgressBar.setVisibility(View.VISIBLE);

//			mDriveServiceHelper.resetQuery();
//			setQStateInprogress();

		try {
//			mQuerier.getState().query();
			treeOpener.getItems().fetch();
		} catch (MissingDriveClientException | GeneralServiceException e) {
			Log.w(TAG, e.getMessage());
			Log.w(TAG, e.getCause());
			mProgressBar.setVisibility(View.INVISIBLE);
			Toast.makeText(getActivity().getApplicationContext(), e.getMessage() + e.getCause(), Toast.LENGTH_LONG).show();
		}
	}

//	final Observer<ArrayList<SerachResultItemModel>> listChangeObserver  = new Observer<ArrayList<SerachResultItemModel>>(){
//
//		@Override
//		public void onChanged(ArrayList<SerachResultItemModel> items) {
//			int size = 0 ;
//			if(items != null){ size = items.size();}
//			Log.d(TAG, "Live data available. Number of items: " + size);
//
//			mItems.addAll(items);
//			mAdapter.notifyDataSetChanged();
//
//			isQueryOngoing = false;
//			mProgressBar.setVisibility(View.INVISIBLE);
//		}
//	};

	private void queryFileContinue(){
		ArrayList<SerachResultItemModel> items = treeOpener.getItems().getValue();

		//We are reaching the end of list. Stop query.
		//We are okay because no filter is applied.
//		if (getState() == QSTATE_EOL) {
//			CloseQuery();
//			Log.d(TAG, "End of List. Exit directly");
//			return;
//		}

		Log.d(TAG, "Querying for files continue.");

		//mProgressBar.setVisibility(View.VISIBLE);

		//Insert a null item so that the adapter knows that progress bar needs to be shown to the user
		items.add(null);
		Log.d(TAG, "Notify inserted");
		//mAdapter.notifyItemInserted(items.size() - 1);

		//mDriveServiceHelper.queryFiles()
		try {
			//TODO: will be removed
			treeOpener.getItems().fetch();
//			CDFS.getCDFSService().getService().list(mNextPage)
//					.addOnSuccessListener(new OnSuccessListener<com.crossdrives.cdfs.common.Result>() {
//						@Override
//						public void onSuccess(com.crossdrives.cdfs.common.Result result) {
//							List<File> f = result.getFileList().getFiles();
//							int i = 0;
//							//now we are done with the query. take out the progress bar from the list
//							Log.i(TAG, "Notify removed");
//							mItems.remove(mItems.size() - 1);
//							mAdapter.notifyItemRemoved(mItems.size());
//
//							Log.i(TAG, "Number of files fetched: " + f.size());
//
//							for (File file : result.getFileList().getFiles()) {
////                                if(file.getModifiedTime() == null){
////                                    Log.w(TAG, "Modified dateTime is null");
////                                }
//								//ItemModelBase item = mItems.get(i);
//								mItems.add(new SerachResultItemModel(false, file.getName(), file.getId(), file.getModifiedTime(),false));
//								//item.setName(file.getName());
//								i++;
//							}
//
//							//now update adapter
//							//mAdapter.updateRecords(mItems);
//							Log.d(TAG, "Notify data set change");
//							//TODO: to clarify why the newly loaded items are not updated to screen if we dont do any further scroll.
//							// i.e. enter the recycler view from previous screen and only few items are initially loaded
//							mAdapter.notifyDataSetChanged();
//
//							mNextPage = result.getFileList().getNextPageToken();
//							if(mNextPage == null){
//								Log.d(TAG, "Next page handler is null!");
//								CloseQuery();
//							}
//						}
//					})
//					.addOnFailureListener(new OnFailureListener() {
//						@Override
//						public void onFailure(@NonNull Exception exception) {
//							//mProgressBar.setVisibility(View.GONE);
//							Log.e(TAG, "Unable to query files.", exception);
//							//TODO: Has to find out a way to catch UserRecoverableAuthIOException. The handling code example can be found at:
//							//https://stackoverflow.com/questions/15142108/android-drive-api-getting-sys-err-userrecoverableauthioexception-if-i-merge-cod
//						}
//					});

		} catch (MissingDriveClientException | GeneralServiceException e) {
			Log.w(TAG, e.getMessage());
			Log.w(TAG, e.getCause());
			mProgressBar.setVisibility(View.INVISIBLE);
		}
		//}
	}

//	abstract class State{
//		Querier querier;
//		public State(Querier querier) {
//			this.querier = querier;
//		}
//
//		public abstract void query() throws GeneralServiceException, MissingDriveClientException;
//	}
//
//	class ReadyState extends State{
//		public ReadyState(Querier querier) {
//			super(querier);
//		}
//
//		@Override
//		public void query() throws GeneralServiceException, MissingDriveClientException {
//			Log.d(TAG, "State Ready.");
//			querier.changeState(new FetchingState(querier));
//			treeOpener.open(null);	//set null to query the items in base folder
//			treeOpener.fetchAsync();
//			//fetchList.fetchAsync(mParents, null);
//		}
//	}
//
//	class FetchingState extends State{
//
//		public FetchingState(Querier querier) {
//			super(querier);
//		}
//
//		@Override
//		public void query() throws GeneralServiceException, MissingDriveClientException {
//			Log.d(TAG, "State fetching.");
//			if (treeOpener.endOfList()){
//				querier.changeState(new EndState(querier));
//				return;
//			}
//
//			treeOpener.fetchAsync();
//		}
//	}
//
//	class EndState extends State{
//
//		public EndState(Querier querier) {
//			super(querier);
//		}
//
//		@Override
//		public void query() {
//			Log.d(TAG, "State End.");
//			querier.changeState(new ReadyState(querier));
//		}
//	}
//	class Querier{
//		State state;
//
//		public Querier() {
//			this.state = new ReadyState(this);
//		}
//
//		void changeState(State state){
//			this.state = state;
//		};
//
//		State getState(){return this.state;}
//	}

	private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
		@Override
		public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
			super.onScrollStateChanged(recyclerView, newState);
			Log.d(TAG, "onScrollStateChanged");
		}

		/*
			This method gets called if and only if items have been added to recycler view.
		 */
		@Override
		public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
			super.onScrolled(recyclerView, dx, dy);

			List<SerachResultItemModel> currList = mAdapter.getCurrentList();
			OpenTree.ItemLiveData liveData = treeOpener.getItems();
			LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

			//Log.d(TAG, "Onscroll mItems.Size:" + mItems.size());

			//fetch next page if last item is already shown to the user
			if (linearLayoutManager == null){return;};

//			if(isQueryOngoing == true) {
//				Log.d(TAG, "Skip requested query since previous one not yet done.");
//				return;
//			}

			//size of item list(mItems) is always more than 1 because onScrolled only gets called if item has been added
			//to recycler view.
			if(linearLayoutManager.findLastCompletelyVisibleItemPosition() == currList.size() - 1) {
				Log.d(TAG, "Reach EOL. Fetch next part of list.");
				liveData.fetch();
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
		Log.d(TAG, "initialQuery...");
		treeOpener.open(null);
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
	HashMap<IDownloadProgressListener, Notification> mNotificationsByDownloadListener = new HashMap<>();
	HashMap<OnSuccessListener, Notification> mDownloadSuccessListener = new HashMap<>();
	HashMap<OnFailureListener, Notification> mDownloadFailedListener = new HashMap<>();
	CompletableFuture<Boolean> requestPermissionFuture;
	Permission permission;
	private RootItemsAdapter.Notifier AdapterNotifier = new RootItemsAdapter.Notifier() {
	//private QueryFileAdapter.OnItemClickListener itemClickListener = new QueryFileAdapter.OnItemClickListener() {
		@Override
		public void onItemClick(RootItemsAdapter adapter, View view, int position){
			List<SerachResultItemModel> list = adapter.getCurrentList();
			Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Pressed!", Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Short press item:" + position);
			Log.d(TAG, "Count of selected:" + mSelectedItemCount);
			SerachResultItemModel item = list.get(position);

			if (view == view.findViewById(R.id.iv_more_vert)) {
				Log.d(TAG, "More_vert pressed!");
			}

			if (mState == STATE_NORMAL) {
				requestPermissionFuture = new CompletableFuture<>();
				requestPermissionFuture.thenAccept((isGranted)->{
					if(!isGranted){
						Log.w(TAG, "User denied to grant the permission. Skip the requested download.");
						return;
					}
					Open.download(getActivity(), item, treeOpener.whereWeAre());
//					Log.d(TAG, "Start to download file: " + item.mName);
//					Toast.makeText(getContext(), getString(R.string.toast_action_taken_download_start), Toast.LENGTH_LONG).show();
//					//Log.d(TAG, "File ID: " + item.mId);
//					//TODO: open detail of file
//					Notification notification
//					= new Notification(Notification.Category.NOTIFY_DOWNLOAD, R.drawable.ic_baseline_cloud_circle_24);
//					notification.setContentTitle(getString(R.string.notification_title_downloading));
//					notification.setContentText(getString(R.string.notification_content_default));
//					notification.build();
//					ResultUpdater resultUpdater = new ResultUpdater();
//					OnSuccessListener<String> successListener = resultUpdater.createDownloadSuccessListener(notification);//createDownloadSuccessListener();
//					OnFailureListener failureListener = resultUpdater.createDownloadFailureListener(notification);
//					IDownloadProgressListener downloadProgressListener = new ProgressUpdater().createDownloadListener(notification);
////					mNotificationsByDownloadListener.put(downloadProgressListener, notification);
////					mDownloadSuccessListener.put(successListener, notification);
////					mDownloadFailedListener.put(failureListener, notification);
//					//Log.d(TAG, "Put progress listener. listener:" + mNotificationsByDownloadListener);
//					Service service = CDFS.getCDFSService(getActivity().getApplicationContext()).getService();
//					if (service != null) {
//						service.setDownloadProgressListener(downloadProgressListener);
//					}
//					try {
//						service.download(item.getID(), whereWeAre).addOnSuccessListener(successListener)
//								.addOnFailureListener(failureListener);
//					} catch (MissingDriveClientException | PermissionException e) {
//						Toast.makeText(getContext(), "file download failed! " + e.getMessage(), Toast.LENGTH_LONG).show();
//					}
				});

				permission = new Permission(FragmentManager.findFragment(view), requestPermissionLauncher,
						Manifest.permission.WRITE_EXTERNAL_STORAGE).
						setEducationMessage(getString(R.string.message_permission_education)).
						setImplicationMessage(getString(R.string.implication_deny_grant_permission_external_storage));
				boolean permissionGranted = permission.request();
				//if result is false, requested permission has not yet or user response with "never ask again"
				//
				if(permissionGranted == true){
					requestPermissionFuture.complete(true);
					return;
				}
			} else {
				/*
                    An entry is tapped. Distinguish whether the entry is already in selected state or not.
                    1. click on the item in selected state, again exit item selection state
                    2. User likes to select another item or open a folder
                */
				if (item.isSelected()) {
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

			mAdapter.notifyItemChanged(position);
			//mAdapter.notifyDataSetChanged();
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		}

		/*
			Once user selected "never ask again" checkbox, nothing is shown even the requestPermissionLauncher.launch
            is called. Besides, the callback is called with negative isGranted (false).
        */
		private ActivityResultLauncher<String> requestPermissionLauncher =
				registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        Log.d(TAG, "User granted permission.");
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        Log.d(TAG, "Show the implication.");
						String message = getString(R.string.implication_deny_grant_permission_external_storage);
						if(permission.neverAskAgainSelected()){
							message = getString(R.string.implication_permission_external_storage_denied_never_ask);
						}
						Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
					requestPermissionFuture.complete(isGranted);
                });

		@Override
		public void onItemLongClick(RootItemsAdapter adapter, View view, int position) {
			List<SerachResultItemModel> list = adapter.getCurrentList();
			Toast.makeText(view.getContext(), "Position" + Integer.toString(position) + "Long Pressed!", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Long press item:" + position);
			Log.i(TAG, "Count of selected:" + mSelectedItemCount);

			SerachResultItemModel item = list.get(position);

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
			mAdapter.notifyItemChanged(position);
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		}

		@Override
		public void onImageItemClick(RootItemsAdapter adapter, View view, int position) {
			Log.i(TAG, "onImageItemClick:" + position);
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
			PopupMenu popup = new PopupMenu(getContext(), view);
			popup.setOnMenuItemClickListener(com.crossdrives.ui.listener.PopupMenu.create());
			MenuInflater inflater = popup.getMenuInflater();
			inflater.inflate(R.menu.menu_overflow_popup, popup.getMenu());
			popup.show();
		}

		@Override
		public void onCurrentListChanged(RootItemsAdapter adapter, List<SerachResultItemModel> list) {

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
		List<SerachResultItemModel> currList = mAdapter.getCurrentList();

		for(Iterator iter = currList.iterator();iter.hasNext();) {
			SerachResultItemModel item = (SerachResultItemModel) iter.next();
			if(item.isSelected()) {
				item.setSelected(false);
				mSelectedItemCount--;
			}
		}

	}

	View.OnClickListener onHeaderClick = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Fragment f = FragmentManager.findFragment(mView);
			Log.d(TAG, "header is clicked");
			mCountPressDrawerHeader++;

			NavDirections a = QueryResultFragmentDirections.navigateToSystemTest();
			NavHostFragment.findNavController(f).navigate(a);
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
	}};

	View.OnClickListener onFabClick = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Log.d(TAG, "fab is clicked");

			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//			Intent intent = new Intent(FragmentManager.findFragment(v).getActivity(), FABOptionDialog.class);
//			//intent.putExtra("Brand", SignInManager.BRAND_MS);
//			//mStartForResult.launch(intent);
//			FABOptionAlertDialog dialog = new FABOptionAlertDialog();
//			dialog.show(getParentFragmentManager(), "FABOptionAlertDialog");
		}
	};

	View.OnClickListener onScrimClick = new View.OnClickListener(){

		@Override
		public void onClick(View view) {
			Log.d(TAG, "onScrimClick");
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
		}
	};

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

	NavigationView.OnNavigationItemSelectedListener OnNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			// Handle navigation view item clicks here.
			int id = item.getItemId();

//			if(item.isChecked())
//				Log.d(TAG, "checked!");

			//close drawer right here. Otherwise, the drawer is still there if screen is switched back from next one
			mDrawer.closeDrawers();
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

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
	};

	NavigationView.OnNavigationItemSelectedListener OnBottomNavItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			// Handle navigation view item clicks here.
			int id = item.getItemId();

//			if(item.isChecked())
//				Log.d(TAG, "checked!");

			//close bottom sheet here. Otherwise, the sheet is still there if screen is switched back from next one
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

			//The screen transition will take place in callback onDrawerClosed. This is because we have to ensure that the
			//drawer is closed exactly before screen proceed to next one
			Log.d(TAG, "Bottom sheet item is clicked: ");
			if (id == R.id.bottom_sheet_menu_item_upload_file) {
				Log.d(TAG, "Item upload.");
				String[] filter = new String[]{"*/*"}; // Set filter to all types. Null will lead to crash.
				mStartOpenDocument.launch(filter);
			}else if (id == R.id.bottom_sheet_menu_item_create_folder){

				Log.d(TAG, "Item create folder.");
//				CreateFolderAlertDialog dialog = new CreateFolderAlertDialog();
//				dialog.show(getActivity().getSupportFragmentManager(), "BaseActionDialog");

				// We are safe to use mActivity here because onViewCreated() is invoked. i.e. mActivity is
				// initialized in onViewCreated()
				CreateFolderDialogBuilder builder = new CreateFolderDialogBuilder();
				builder.setTitle("Create Folder").setContent("Enter folder name").setNumTextInputBox(1);
				mStartForResult.launch(builder.build(mActivity));
			}else{
				Log.w(TAG, "Unknown item detected!");
			}
			return true;
		}
	};

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
			menu.findItem(R.id.miDelete).setOnMenuItemClickListener(OnMenuItemDeleteClickListener);
			menu.findItem(R.id.miDetail).setOnMenuItemClickListener(OnMenuItemClickListener);

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

	//Action bar icon: delete
	private MenuItem.OnMenuItemClickListener OnMenuItemDeleteClickListener = new MenuItem.OnMenuItemClickListener(){

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			Task task = null;
			List<SerachResultItemModel> currList = mAdapter.getCurrentList();
			OnSuccessListener<com.crossdrives.driveclient.model.File> deleteSuccessListener;
			OnFailureListener deleteFailureListener;
			IDeleteProgressListener progressListener;

			Toast.makeText(getContext(), getString(R.string.toast_action_taken_delete_start), Toast.LENGTH_LONG).show();

			Notification notification
					= new Notification(Notification.Category.NOTIFY_DELETE, R.drawable.ic_baseline_cloud_circle_24);
			notification.setContentTitle(getString(R.string.notification_title_deleting));
			notification.setContentText(getString(R.string.notification_content_default));
			notification.build();
			progressListener = new ProgressUpdater().createDeleteListener(notification);

			Service service = CDFS.getCDFSService().getService();
			service.setDeleteProgressListener(progressListener);

			//get the item checked
			SerachResultItemModel selectedItem = currList.stream().filter((i)->{
				return i.isSelected();
			}).findFirst().get();
			selectedItem.setSelected(false);
			if(!currList.remove(selectedItem)){
				Log.d(TAG, "Failed to remove item from list!");
			}
			mAdapter.notifyDataSetChanged();

			Log.d(TAG, "Delete item: " + selectedItem.getName());
			try {
				task = service.delete(selectedItem.getID(), treeOpener.whereWeAre());
			} catch (MissingDriveClientException | PermissionException e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG);
				Log.w(TAG, e.getMessage());
				e.printStackTrace();
				return true;
			}

			ResultUpdater resultUpdater = new ResultUpdater();
			deleteSuccessListener = resultUpdater.createDeleteSuccessListener(notification);
			deleteFailureListener = resultUpdater.createDeleteFailureListener(notification);
			task.addOnSuccessListener(deleteSuccessListener).
			addOnFailureListener(deleteFailureListener);

			mAdapter.setOverflowIconVisible(true);
			mState = STATE_NORMAL;
			exitActionMode(getView());

			return false;
		}
	};

	OnSuccessListener<File> deleteSuccessListner = new OnSuccessListener<File>() {
		@Override
		public void onSuccess(File file) {
			Toast.makeText(getContext(), file.getName() + " is deleted successfully.", Toast.LENGTH_LONG);
		}
	};

	OnFailureListener deleteFailureListener = new OnFailureListener() {
		@Override
		public void onFailure(@NonNull Exception e) {
			Toast.makeText(getContext(), " something wrong during deleting the file. " + e.getMessage(), Toast.LENGTH_LONG);
		}
	};

	//Action bar: overflow menu items
	private MenuItem.OnMenuItemClickListener OnMenuItemClickListener = new MenuItem.OnMenuItemClickListener(){
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			//YourActivity.this.someFunctionInYourActivity();
			Log.d(TAG, "Top app bar menu item action pressed!!");
			return true;
		}


	};

	private Toolbar.OnMenuItemClickListener onBottomAppBarMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			//YourActivity.this.someFunctionInYourActivity();
			Log.d(TAG, "Bottom app bar menu item action pressed!!");
			return true;
		}
	};

	private ActivityResultLauncher<String[]> mStartOpenDocument = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
			new ActivityResultCallback<Uri>() {
				@Override
				public void onActivityResult(Uri result) {
					java.io.File file;
					InputStream in = null;
					Task task;
					Service service;
					IUploadProgressListener uploadListener;
					OnSuccessListener<com.crossdrives.driveclient.model.File> successListener;
					OnCompleteListener<Task> completeListener;
					OnFailureListener failureListener;
					Notification notification;

					if(result != null) {
						try {
							in = getActivity().getContentResolver().openInputStream(result);
						} catch (FileNotFoundException e) {
							Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
							return;
						}
						notification
								= new Notification(Notification.Category.NOTIFY_UPLOAD, R.drawable.ic_baseline_cloud_circle_24);
						notification.setContentTitle(getString(R.string.notification_title_uploading));
						notification.setContentText(getString(R.string.notification_content_default));
						notification.build();

						file = UriToFile(result);
						String name = file.getPath();	//Note name is stored in path returned from UriToFile
						Log.d(TAG, "Name of file to upload: " + file.getPath());
						try {
							service = CDFS.getCDFSService().getService();
							/*
								Next 3 lines of code are used only if you want to use test file for upload
							 */
//							name = "TestFile";
//							in = new TestFileGenerator(name, 4*1024*1024).run();
//							in = getContext().openFileInput(name);
//							Log.d(TAG, "Test file used. file: " + name +
//									" Length:" + in.available());

							task = service.upload(in, name, treeOpener.getParents());
							InputStream finalIn = in;
							/*
							* Setup listeners
							* */
							uploadListener = new ProgressUpdater().createUploadListener(notification);
							//mNotificationsByUploadListener.put(uploadListener, notification);
							service.setUploadProgressLisetener(uploadListener);
							ResultUpdater resultUpdater = new ResultUpdater();
							successListener = resultUpdater.createUploadSuccessListener(notification);
							failureListener = resultUpdater.createUploadFailureListener(notification);
							completeListener = resultUpdater.createUploadCompleteListener(finalIn);
//							mNotificationsByUpFailedListener.put(failureListener, notification);
//							mNotificationsByUploadSuccessListener.put(successListener, notification);
							task.addOnCompleteListener(completeListener)
									.addOnFailureListener(failureListener)
									.addOnSuccessListener(successListener);
						} catch (Exception e ) {
							Toast.makeText(getActivity().getApplicationContext(), e.getMessage() + e.getCause(), Toast.LENGTH_LONG).show();
						}
						Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_action_taken_upload_start), Toast.LENGTH_LONG).show();
					}
				}
			});

	ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				@Override
				public void onActivityResult(ActivityResult result) {
					//Result code could be altered: https://medium.com/mobile-app-development-publication/undocumented-startactivityforresult-behavior-for-fragment-b7b04d24a346
					if (result.getResultCode() == Activity.RESULT_OK) {
						ArrayList<String> results;
						Task<com.crossdrives.driveclient.model.File> task;
						OnSuccessListener<com.crossdrives.driveclient.model.File> successListener;
						OnFailureListener failureListener;
 						Intent intent = result.getData();
						CreateFolderDialogResultResolver resolver = new CreateFolderDialogResultResolver();
						results = resolver.getNames(intent);
						Log.d(TAG, "folder name entered: " + results.get(0));
						try {
							task = CDFS.getCDFSService().getService().create(results.get(0), treeOpener.getParents());
							ResultUpdater resultUpdater = new ResultUpdater();
							successListener = resultUpdater.createCreateSuccessListener(null);
							failureListener = resultUpdater.createCreateFailureListener(null);
							task.addOnSuccessListener(successListener).addOnFailureListener(failureListener);
						} catch (Exception e) {
							Toast.makeText(getActivity().getApplicationContext(), e.getMessage() + e.getCause(), Toast.LENGTH_LONG).show();
						}

					}
				}
			});
	//https://www.jb51.net/article/112581.htm
	private java.io.File UriToFile(final Uri uri){
		if ( null == uri ) return null;
		final String scheme = uri.getScheme();
		Log.d(TAG, "document Uri: " + uri.getEncodedPath());
		Log.d(TAG, "document scheme: " + scheme);

		String data = null;
		if ( scheme == null )
			data = uri.getPath();
		else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
			data = uri.getPath();
		}
		else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
			Cursor cursor = SnippetApp.getAppContext().getContentResolver().query(
					uri, new String[] { MediaStore.Files.FileColumns.DISPLAY_NAME }, null, null, null );
			if ( null != cursor ) {
				if ( cursor.moveToFirst() ) {
					int index = cursor.getColumnIndex( /*MediaStore.Video.Media.DISPLAY_NAME*/
							MediaStore.Files.FileColumns.DISPLAY_NAME);
					if ( index > -1 ) {
						data = cursor.getString( index );
						if(data == null) {Log.w(TAG, "query result is null");	}
					}
					else{
						Log.w(TAG, "column doesnt exist");
					}
				}
				else{
					Log.w(TAG, "cursor is empty");
				}
				cursor.close();
			}
			else{
				Log.w(TAG, "cursor is null");
			}
		}
		return new java.io.File(data);
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		Log.d(TAG, "User input folder: ");
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {

	}
}