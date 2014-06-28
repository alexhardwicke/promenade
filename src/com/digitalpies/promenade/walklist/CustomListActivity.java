package com.digitalpies.promenade.walklist;

import java.io.File;
import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Photo;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.dialogue.DeleteWalkDialogue;
import com.digitalpies.promenade.dialogue.DeletingWalksDialogue;
import com.digitalpies.promenade.dialogue.SortListDialogue;
import com.digitalpies.promenade.dialogue.WalkDetailsDialogue;
import com.digitalpies.promenade.dialogue.WalkDialogue;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.OnItemClickListener;

/**
 * An abstract implementation of ListActivity. Extended by both WalkListActivity and SearchableActivity.<br>
 * <br>
 * Contains all of the methods that both classes use: handling taps and long presses on list items, deleting
 * walks (and showing a progress dialogue), editing walks and sorting the list.<br>
 * <br>
 * Also contains two private classes for selecting multiple items and deleting photos via AsyncTask.
 * 
 * @author Alex Hardwicke
 */
public abstract class CustomListActivity extends ListActivity implements OnItemClickListener
{
	protected ArrayList<Walk> walks = new ArrayList<Walk>();
	protected SharedPreferences preferences;
	protected ListAdapter adapter;

	private ProgressDialog deleteDialog;
	private static DeleteTask deleteTask;

	public final static String WALK_TAG = "WALK_TAG";

	public final static String NEW_WALK_TASK = "NEW_WALK_TASK";
	public final static String EDIT_WALK_TASK = "EDIT_WALK_TASK";

	public static final String DIALOGUE_FRAGMENT_DELETE = "DIALOGUE_FRAGMENT_DELETE";
	public static final String DIALOGUE_FRAGMENT_EDIT_WALK = "DIALOGUE_FRAGMENT_EDIT_WALK";
	public static final String DIALOGUE_FRAGMENT_SAVE_WALK = "DIALOGUE_FRAGMENT_SAVE_WALK";
	public static final String DIALOGUE_FRAGMENT_DELETING_WALKS = "DIALOGUE_FRAGMENT_DELETING_WALKS";
	public static final String DIALOGUE_FRAGMENT_TAGS = "DIALOGUE_FRAGMENT_TAGS";
	public static final String DIALOGUE_FRAGMENT_ENABLE_GPS = "DIALOGUE_FRAGMENT_ENABLE_GPS";
	public static final String DIALOGUE_FRAGMENT_SHOW_NEW_WALK = "DIALOGUE_FRAGMENT_SHOW_NEW_WALK";
	public static final String DIALOGUE_FRAGMENT_WAITING_FOR_GPS = "DIALOGUE_FRAGMENT_WAITING_FOR_GPS";
	public static final String DIALOGUE_FRAGMENT_SORT_LIST = "DIALOGUE_FRAGMENT_SORT_LIST";
	public static final String DIALOGUE_FRAGMENT_DELETE_TAGS = "DIALOGUE_FRAGMENT_DELETE_TAGS";
	public static final String DIALOGUE_FRAGMENT_ADD_NOTE = "DIALOGUE_FRAGMENT_ADD_NOTE";
	public static final String DIALOGUE_FRAGMENT_EDIT_NOTE = "DIALOGUE_FRAGMENT_EDIT_NOTE";
	public static final String DIALOGUE_FRAGMENT_DELETE_NOTE = "DIALOGUE_FRAGMENT_DELETE_NOTE";
	public static final String DIALOGUE_FRAGMENT_DELETE_PHOTO = "DIALOGUE_FRAGMENT_DELETE_PHOTO";
	public static final String DIALOGUE_FRAGMENT_PHOTO = "DIALOGUE_FRAGMENT_PHOTO";
	public static final String DIALOGUE_FRAGMENT_RESET_TOASTS = "DIALOGUE_FRAGMENT_RESET_TOASTS";
	public static final String DIALOGUE_FRAGMENT_SHOW_NOTE = "DIALOGUE_FRAGMENT_SHOW_NOTE";
	public static final String DIALOGUE_FRAGMENT_REMOVE_TAG_LIST = "DIALOGUE_FRAGMENT_REMOVE_TAG_LIST";
	public static final String DIALOGUE_FRAGMENT_CANCEL_WALK = "DIALOGUE_FRAGMENT_CANCEL_WALK";

	// Used when sorting the list
	public final static int DATE_ASCENDING = 0;
	public final static int DATE_DESCENDING = 1;
	public final static int NAME_ASCENDING = 2;
	public final static int NAME_DESCENDING = 3;
	public static final String DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS = "DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS";

	// ///////////////////////
	//
	// Initialisation, pause and resume methods
	//
	// ///////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Make sure that the DataSource is open
		DataSource.openDataSource(this);

		// Get access to the app's preferences
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Set up the ListView
		ListView listView = getListView();
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new CustomMultiChoiceModeListener());

		// Setting the ActionBar backgrounds
		BitmapDrawable bitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.stripes);
		bitmap.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		getActionBar().setBackgroundDrawable(bitmap);
		getActionBar().setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.purple_dot));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// If walks are being deleted, set the activity (so that the dialogue can be updated)
		if (CustomListActivity.deleteTask != null) CustomListActivity.deleteTask.setActivity(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		// If walks are being deleted, clear the activity (to prevent it trying to access a dead activity)
		if (CustomListActivity.deleteTask != null) CustomListActivity.deleteTask.setActivity(null);
	}

	// ///////////////////////
	//
	// Dialogue methods
	//
	// ///////////////////////
	/**
	 * Allows the user to delete a walk. The walk is already set in a variable in the code, so the method
	 * has no need to take a walk as a parameter.
	 */
	protected void showDeleteWalk(ArrayList<Walk> selectedWalks)
	{
		boolean photos = false;
		for (Walk walk : selectedWalks)
		{
			if (DataSource.getPhotoCountForWalk(walk) > 0)
			{
				photos = true;
				break;
			}
		}

		// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
		FragmentManager manager = getFragmentManager();
		if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_DELETE) == null)
		{
			DialogFragment newFragment = DeleteWalkDialogue.newInstance(selectedWalks, photos);
			newFragment.show(manager, DIALOGUE_FRAGMENT_DELETE);
		}
	}

	/**
	 * Shows the "Sort List" dialogue
	 */
	public void showSort()
	{
		// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
		FragmentManager manager = getFragmentManager();
		if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_SORT_LIST) == null)
		{
			DialogFragment newFragment = SortListDialogue.newInstance();
			newFragment.show(manager, DIALOGUE_FRAGMENT_SORT_LIST);
		}
	}

	// ///////////////////////
	//
	// Walk modifying methods
	//
	// ///////////////////////
	/**
	 * Starts an AsyncTask to delete provided walks, and optionally the photos if deletePhotos is true.
	 * 
	 * @param selectedWalks	An ArrayList<Walk> of walks to be deleted.
	 * @param deletePhotos	True if the user wants the photos to be deleted
	 */
	public void deleteWalk(final ArrayList<Walk> selectedWalks, boolean deletePhotos)
	{
		// Create the AsyncTask
		CustomListActivity.deleteTask = new DeleteTask(this, deletePhotos, selectedWalks);

		// Show a progress dialogue
		// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
		FragmentManager manager = getFragmentManager();
		if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_DELETING_WALKS) == null)
		{
			DialogFragment newFragment = DeletingWalksDialogue.newInstance(selectedWalks.size());
			newFragment.show(manager, DIALOGUE_FRAGMENT_DELETING_WALKS);
		}

		// Start the AsyncTask
		CustomListActivity.deleteTask.execute();
	}

	/**
	 * Updates the provided walk and updates the list.
	 * 
	 * @param walk	The edited walk.
	 */
	public void editWalk(Walk walk)
	{
		DataSource.editWalk(walk);
		updateWalksList();
	}

	// ///////////////////////
	//
	// Other methods
	//
	// ///////////////////////
	/**
	 * Opens a MapViewActivity for the walk the user has tapped on.
	 */
	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3)
	{
		Intent intent = new Intent(this, com.digitalpies.promenade.maps.MapViewActivity.class);
		intent.putExtra(WALK_TAG, this.walks.get(position));
		startActivity(intent);
	}

	@Override
	public boolean onSearchRequested()
	{
		return false;
	}

	public abstract void updateWalksList();

	// ///////////////////////
	//
	// AsyncTask methods
	//
	// ///////////////////////
	/**
	 * Run by the delete AsyncTask each time a walk is deleted.<br>
	 * <br>
	 * Retrieves the dialogue if it's null, and then updates the dialogue's progress.
	 * 
	 * @param args	A one dimensional array with only one value - the new progress position.
	 */
	public void onTaskUpdated(Integer[] args)
	{
		if (this.deleteDialog == null)
			this.deleteDialog = ((ProgressDialog) ((DialogFragment) getFragmentManager().findFragmentByTag(
					DIALOGUE_FRAGMENT_DELETING_WALKS)).getDialog());
		this.deleteDialog.setProgress(args[0]);
	}

	/**
	 * Run by the delete AsyncTask when it's successfully completed.<br>
	 * <br>
	 * Dismisses the dialogue, clears the AsyncTask, and updates the walks list.
	 */
	public void onTaskCompleted()
	{
		if (this.deleteDialog == null)
			this.deleteDialog = ((ProgressDialog) ((DialogFragment) getFragmentManager().findFragmentByTag(
					DIALOGUE_FRAGMENT_DELETING_WALKS)).getDialog());
		this.deleteDialog.dismiss();
		this.deleteDialog = null;

		CustomListActivity.deleteTask = null;
		updateWalksList();
	}

	/**
	 * Run by the delete AsyncTask if the user cancels it at any point.<br>
	 * <br>
	 * Sets cancel to true in the AsyncTask, sets the reference to null, and updates the walks list.
	 */
	public void cancelDeletion()
	{
		if (CustomListActivity.deleteTask != null)
		{
			CustomListActivity.deleteTask.cancel(true);
			CustomListActivity.deleteTask = null;
		}
		updateWalksList();
	}

	// ///////////////////////
	//
	// Inner classes
	//
	// ///////////////////////
	/**
	 * A custom MultiChoiceModeListener used when the user long-presses walks.<br>
	 * <br>
	 * Manages a selection menu with edit walk and delete walk options. If one item is
	 * selected, both the edit walk and delete walk options are available. If more than
	 * one item is selected, only the delete walk option is available.
	 * 
	 * @author Alex Hardwicke
	 */
	protected class CustomMultiChoiceModeListener implements MultiChoiceModeListener
	{
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			// Get the checked items
			SparseBooleanArray sba = getListView().getCheckedItemPositions();
			switch (item.getItemId())
			{
			case R.id.edit_walk_button:
				for (int i = 0; i < sba.size(); i++)
				{
					// If this item is checked
					if (sba.valueAt(i))
					{
						// Show the edit walk dialogue and break the loop.
						// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
						// button)
						FragmentManager manager = getFragmentManager();
						if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_EDIT_WALK) == null)
						{
							DialogFragment newFragment = WalkDialogue.newInstance(EDIT_WALK_TASK,
									CustomListActivity.this.walks.get(sba.keyAt(i)));
							newFragment.show(manager, DIALOGUE_FRAGMENT_EDIT_WALK);
						}
						break;
					}
				}
				mode.finish();
				return true;
			case R.id.view_walk_details_button:
				for (int i = 0; i < sba.size(); i++)
				{
					// If this item is checked
					if (sba.valueAt(i))
					{
						// Show the view walk dialogue and break the loop.
						// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
						// button)
						FragmentManager manager = getFragmentManager();
						if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS) == null)
						{
							DialogFragment newFragment = WalkDetailsDialogue.newInstance(CustomListActivity.this.walks
									.get(sba.keyAt(i)));
							newFragment.show(manager, DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS);
						}
						break;
					}
				}
				mode.finish();
				return true;
			case R.id.delete_walk_button:
				// Create an ArrayList<Walk> and add all of the selected walks to it, and then pass that
				// on to showDeleteWalk.
				ArrayList<Walk> selectedWalks = new ArrayList<Walk>();
				for (int i = 0; i < sba.size(); i++)
				{
					if (sba.valueAt(i)) selectedWalks.add(CustomListActivity.this.walks.get(sba.keyAt(i)));
				}
				showDeleteWalk(selectedWalks);
				mode.finish();
				return true;
			}
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			// Inflate the menu
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.list_contextual_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
		}

		/**
		 * Run when preparing the menu. Updates the title to show how many items are selected,
		 * and the checks how many items are selected. If one, enable edit walk and set the
		 * delete walk button to singular. If two, disable edit walk and set the delete walk
		 * button to plural.
		 */
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			int selectedItemCount = getListView().getCheckedItemCount();
			if (selectedItemCount != 0)
			{
				if (selectedItemCount == 1)
				{
					mode.setTitle(selectedItemCount + " " + getString(R.string.selected));
					menu.getItem(0).setVisible(true);
					menu.getItem(1).setVisible(true);
					menu.getItem(2).setTitle(R.string.delete_walk);
				}
				else
				{
					mode.setTitle(selectedItemCount + " " + getString(R.string.selected_plural));
					if (selectedItemCount == 2)
					{
						menu.getItem(0).setVisible(false);
						menu.getItem(1).setVisible(false);
						menu.getItem(2).setTitle(R.string.delete_walks);
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked)
		{
			// Invalidate the mode when a new item is selected
			mode.invalidate();
		}
	}

	/**
	 * An implementation of AsyncTask used to delete photos and update a progress
	 * dialogue while this happens.
	 * 
	 * @author Alex Hardwicke
	 */
	protected class DeleteTask extends AsyncTask<Boolean, Integer, Bundle>
	{
		private CustomListActivity theActivity;
		private boolean completed;
		private boolean deletePhotos;
		private ArrayList<Walk> selectedWalks;
		private boolean shouldShowDeleteErrorToast = false;
		private boolean shownDeleteErrorToast = false;

		/**
		 * Create the AsyncTask.
		 * 
		 * @param customListActivity	The activity instance running the task
		 * @param deletePhotos			Whether photos should be deleted or not
		 * @param selectedWalks			The walks to be deleted
		 */
		protected DeleteTask(CustomListActivity customListActivity, boolean deletePhotos, ArrayList<Walk> selectedWalks)
		{
			this.theActivity = customListActivity;
			this.deletePhotos = deletePhotos;
			this.selectedWalks = selectedWalks;
		}

		/**
		 * Let the activity set the activity (used in onPause/onDestroy/onResume)
		 * 
		 * @param customListActivity	The new activity instance (often null)
		 */
		public void setActivity(CustomListActivity customListActivity)
		{
			this.theActivity = customListActivity;
			if (this.completed) notifyActivityTaskCompleted();
		}

		/**
		 * Used when the activity is complete. If the activity reference isn't null,
		 * run theActivity.onTaskCompleted()
		 */
		private void notifyActivityTaskCompleted()
		{
			if (this.theActivity != null) this.theActivity.onTaskCompleted();
		}

		/**
		 * The main method that is run by the AsyncTask.<br>
		 * <br>
		 * Iterates through the walk list. Checks if cancelled has been set to true. If it
		 * has, it breaks the loop.<br>
		 * <br>
		 * Assuming it hasn't broken, it then checks if it should delete photos. If so, it
		 * gets all photos for the provided walk and iterates through those photos. For each
		 * photo it deletes it, and checks if it succeeded in deleting.<br>
		 * <br>If it didn't, and this is the first photo that failed to delete in this
		 * AsyncTask, it sets shouldShowDeleteErrorToast to true. This is then checked at the
		 * nearest opportunity and a toast is shown to inform the user.<br>
		 * <br>
		 * It then deletes the walk from the Database and publishes the progress (updating the
		 * progress dialogue).
		 */
		@Override
		protected Bundle doInBackground(Boolean... params)
		{
			for (int i = 0; i < this.selectedWalks.size(); i++)
			{
				Walk walk = this.selectedWalks.get(i);
				if (this.deletePhotos)
				{
					ArrayList<Photo> photos = DataSource.getPhotosForWalk(walk);
					for (Photo photo : photos)
					{
						// Stop deleting photo files as soon as the user clicks cancel!
						if (isCancelled()) break;
						File file = new File(photo.getFile());
						boolean deleted = file.delete();
						if (!this.shownDeleteErrorToast && !deleted)
						{
							this.shouldShowDeleteErrorToast = true;
						}
					}
				}
				// Don't delete if cancelled!
				if (isCancelled()) break;
				DataSource.deleteWalk(walk);
				if (this.theActivity != null) publishProgress(i);
			}
			return null;
		}

		/**
		 * Run each time a walk is deleted. Updates the dialogue, and if the shouldShowDeleteErrorToast
		 * boolean is true, shows a Toast telling the user that the task has failed to delete at least
		 * one photo, and then toggles some booleans to prevent this Toast being shown again for this
		 * task.
		 */
		@Override
		protected void onProgressUpdate(Integer... args)
		{
			this.theActivity.onTaskUpdated(args);

			if (this.shouldShowDeleteErrorToast)
			{
				Toast.makeText(getBaseContext(), R.string.error_deleting_photos, Toast.LENGTH_SHORT).show();
				this.shownDeleteErrorToast = true;
				this.shouldShowDeleteErrorToast = false;
			}
		}

		/**
		 * Run once the task is complete. Sets completed to true and runs notifyActivityTaskCompleted.
		 */
		@Override
		protected void onPostExecute(Bundle result)
		{
			this.completed = true;
			notifyActivityTaskCompleted();
		}
	}
}
