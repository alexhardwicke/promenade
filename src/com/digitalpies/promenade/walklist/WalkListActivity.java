package com.digitalpies.promenade.walklist;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.PreferenceActivity;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.dialogue.EnableGPSDialogue;
import com.digitalpies.promenade.dialogue.TagListDialogue;
import com.digitalpies.promenade.dialogue.TagRemoveDialogue;
import com.digitalpies.promenade.dialogue.WalkDialogue;
import com.digitalpies.promenade.gps.GPSService;
import com.digitalpies.promenade.gps.GPSService.LocalBinder;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

/**
 * This Activity displays a ListView containing all the walks saved in the database. It extends the CustomListActivity
 * class for all standard list-related methods for this application.<br>
 *
 * @author Alex Hardwicke
 */
public class WalkListActivity extends CustomListActivity
{
	private boolean filteredByTags = false;
	private boolean inForeground = true; // Set to false when the activity is paused.
	private boolean serviceRunning = false;
	private boolean trackingWalk = false;
	private boolean gpsLocked = false;

	private ArrayList<Tag> checkedTags = null; // A list of Tags used when the user is filtering a walk by tags
	private Intent serviceIntent;
	private MyServiceConnection conn;

	protected GPSService service;

	private final static String LIST_FIRST_POSITION = "LIST_FIRST_POSITION";
	private final static String CHECKED_TAGS = "CHECKED_TAGS";
	private final static String FILTERED_BY_TAGS = "FILTERED_BY_TAGS";

	public static final String FIRST_LIST_RUN = "FIRST_LIST_RUN";

	private final static int GPS_ENABLED = 100;

	// ///////////////////////
	//
	// Initialisation and status change methods
	//
	// ///////////////////////
	/**
	 * Run when the Activity (and application) are created.<br>
	 * <br>
	 * Runs the CustomListActivity constructor, retrieves the walk, sets up the ListAdapter, and restores the
	 * list position if possible.<br>
	 * 
	 * @param savedInstanceState	The savedInstanceState. Only used if the user is rotating their phone (for
	 * 								the list position).
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Getting the walks, creating a WalkListAdapter and setting it as the ListAdapter.
		this.walks = new ArrayList<Walk>();
		this.adapter = new ListAdapter(this, this.walks);
		setListAdapter(this.adapter);

		if (savedInstanceState != null)
		{
			// Setting the ListView position.
			getListView().setSelection(savedInstanceState.getInt(LIST_FIRST_POSITION));
			this.checkedTags = savedInstanceState.getParcelableArrayList(CHECKED_TAGS);
			this.filteredByTags = savedInstanceState.getBoolean(FILTERED_BY_TAGS);
		}
	}

	/**
	 * Run when the Activity is being destroyed.<br>
	 * <br>
	 * If onDestroy is not being run due to a configuration change (so it is actually being closed),
	 * the app is not tracking a walk, and there is an active service, it stops the service.<br>
	 * <br>
	 * Regardless of why onDestroy is being run, if it's bound to the service then it unbinds itself
	 * from it, and runs the super onDestroy.<br>
	 */
	@Override
	public void onDestroy()
	{
		// If the activity is being destroyed by the user (e.g. swiped away in
		// task list) AND it's not currently tracking a walk, then the service
		// should be stopped. Makes sure the intent isn't null for safety's sake.
		if (!isChangingConfigurations() && !this.trackingWalk && this.serviceIntent != null)
		{
			stopService(this.serviceIntent);
		}

		// Unbind the service if it can.
		if (this.conn != null)
		{
			unbindService(this.conn);
			this.conn = null;
		}
		super.onDestroy();
	}

	/**
	 * Run when the Activity is being resumed.<br>
	 * <br>
	 * Runs the super onPause, updates the walk list, and sets inForeground to true. If this is the
	 * first time the user has run the app, shows a Toast telling them to press the shoe prints.
	 * Checks if the service is running. If it isn't, sets gpsLocked and trackingWalk to false. If it
	 * is, it gets the trackingWalk status, and binds itself to the service.<br>
	 * <br>
	 * Finally, invalidates the options menu so that the correct menu is shown.
	 */
	@Override
	public void onResume()
	{
		super.onResume();
		updateWalksList();
		this.setInForeground(true);

		// Check if this is the first time the user has opened the app. If so, inform them with a Toast that
		// they should tap the shoe prints to start, and set the preferences so that this can't happen again
		if (this.preferences.getBoolean(FIRST_LIST_RUN, true))
		{
			Toast.makeText(this, R.string.toast_tap_shoe_prints, Toast.LENGTH_LONG).show();
			Editor editor = this.preferences.edit();
			editor.putBoolean(FIRST_LIST_RUN, false);
			editor.apply();
		}

		// Check if the GPS service is running and if it's tracking
		this.serviceRunning = GPSService.isRunning;

		if (!this.serviceRunning) // If it isn't, set gpsLocked and trackingWalk to false
		{
			this.gpsLocked = false;
			this.trackingWalk = false;
		}
		else
		// If the service is running
		{
			this.trackingWalk = GPSService.trackingWalk; // See if the service is tracking a walk

			this.serviceIntent = new Intent();
			this.serviceIntent.setClassName("com.digitalpies.promenade", "com.digitalpies.promenade.gps.GPSService");

			this.conn = new MyServiceConnection();

			bindService(this.serviceIntent, this.conn, Context.BIND_AUTO_CREATE);
		}
		
		if (GPSService.isCancelled)
		{
			// Get the "New Walk" dialogue.
			DialogFragment newWalkDialogue = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOGUE_FRAGMENT_SHOW_NEW_WALK);
			if (newWalkDialogue != null)
			{
				newWalkDialogue.dismiss();
			}
			// "New Walk" is null, so gpsDialogue should be showing
			else
			{
				DialogFragment gpsDialogue = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOGUE_FRAGMENT_WAITING_FOR_GPS);
				if (gpsDialogue != null)
				{
					gpsDialogue.dismiss();
				}
			}
			GPSService.isCancelled = false;
		}
		invalidateOptionsMenu();
	}

	/**
	 * Run when the Activity is being paused.<br>
	 * <br>
	 * Runs the super onResume, and if the connection isn't null, unbinds itself from the service and sets
	 * the connection to null. Also sets inForeground to false.<br>
	 */
	@Override
	public void onPause()
	{
		super.onPause();

		if (this.conn != null)
		{
			unbindService(this.conn);
			this.conn = null;
		}
		this.setInForeground(false);
	}

	/**
	 * Used to save the Activity's instance state.
	 * <br>
	 * Saves the list position so that it can be maintained when restored (e.g. on rotate), the checkedTags
	 * ArrayList so that tags stay filtered, and the filteredByTags boolean.<br>
	 * 
	 * @param outState	The Bundle data can be saved to.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt(LIST_FIRST_POSITION, getListView().getFirstVisiblePosition());
		outState.putParcelableArrayList(CHECKED_TAGS, this.checkedTags);
		outState.putBoolean(FILTERED_BY_TAGS, this.filteredByTags);
	}
	
	// ///////////////////////
	//
	// OnClick/Menu methods
	//
	// ///////////////////////
	/**
	 * Sets up the activity menu.<br>
	 * <br>
	 * Inflates the menu, enables either the start walk or resume walk button and disables the other,
	 * shows the display all button if the walk is filtered by tags, and sets up the search button.
	 * 
	 * @param Menu	The activity's menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.walk_list_menu, menu);

		menu.findItem(R.id.start_walk_button).setVisible(!this.trackingWalk);
		menu.findItem(R.id.resume_walk_button).setVisible(this.trackingWalk);

		menu.findItem(R.id.display_all_button).setVisible(this.filteredByTags);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(true);

		return true;
	}

	/**
	 * Performs an action depending on the selected menuItem:<br>
	 * <br>
	 * * <i>Start walk:</i>		If GPS is disabled, shows a dialogue for the user asking them if they want to enable
	 * 							GPS. If they do, shows a dialogue asking the user to enter the walk details (name,
	 * 							description & tags), and if the user confirms, launches the MapWalkActivity.<br>
	 * * <i>Resume walk:</i>	Lets the user return to an in-progress walk.
	 * * <i>Filter by tags:</i>	Shows a dialogue containing all tags in all walks as CheckBoxes. If one or more tags
	 *  						are selected and the user presses OK, the list activity is filtered to only show
	 *  						walks which contain at least one of the selected tags.<br>
	 * * <i>Display all:</i>	Removes the tag filter and displays all the walks again.
	 * * <i>Sort list:</i>		Shows a dialogue which allows the user to sort the list by date or name, ascending
	 *  						or descending.<br>
	 * * <i>Remove tags:</i>	Shows a dialogue containing all tags in all walks as CheckBoxes. If one or more tags
	 *  						are selected and the user presses OK, the selected tags are removed from all walks
	 *  						in the database.<br>
	 * * <i>Preferences:</i>	Launches PreferencesActivity.<br>
	 * <br>
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		switch (item.getItemId())
		{
		case R.id.start_walk_button:
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
				showNewWalkDialogue();
			else
			{
				// GPS isn't enabled.
				// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
				FragmentManager manager = getFragmentManager();
				if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_ENABLE_GPS) == null)
				{
					DialogFragment newFragment = EnableGPSDialogue.newInstance();
					newFragment.show(manager, DIALOGUE_FRAGMENT_ENABLE_GPS);
				}
			}
			return true;
		case R.id.resume_walk_button:
			Walk walk = DataSource.getWalkById(0);
			intent = new Intent(this, com.digitalpies.promenade.maps.MapWalkActivity.class);
			intent.putExtra(WALK_TAG, walk);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.tags_button:
			showTagList();
			return true;
		case R.id.display_all_button:
			displayAll();
			return true;
		case R.id.sort_button:
			showSort();
			return true;
		case R.id.tags_remove_button:
			showTagRemoveList();
			return true;
		case R.id.preferences_button:
			intent = new Intent(this, com.digitalpies.promenade.PreferenceActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}

	// ///////////////////////
	//
	// Dialogue methods
	//
	// ///////////////////////
	/**
	 * Starts the service, and then shows a Start New Walk dialogue.
	 */
	private void showNewWalkDialogue()
	{
		// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
		FragmentManager manager = getFragmentManager();
		if (manager.findFragmentByTag(DIALOGUE_FRAGMENT_SHOW_NEW_WALK) == null)
		{
			this.serviceIntent = new Intent();
			this.serviceIntent.setClassName("com.digitalpies.promenade", "com.digitalpies.promenade.gps.GPSService");
			startService(this.serviceIntent);
			this.conn = new MyServiceConnection();
			bindService(this.serviceIntent, this.conn, Context.BIND_AUTO_CREATE);

			DialogFragment newFragment = WalkDialogue.newInstance(NEW_WALK_TASK);
			newFragment.show(manager, DIALOGUE_FRAGMENT_SHOW_NEW_WALK);
		}
	}

	/**
	 * Shows a list of all tags in a dialogue pop-up which enables the user to remove certain tags
	 * from all walks. Each tag is a separate check-box in a scrollable list.
	 */
	private void showTagRemoveList()
	{
		// Get a list of all tags. If there are no tags, show a Toast and end.
		final ArrayList<Tag> tags = DataSource.getAllTags();
		if (tags.size() == 0)
			Toast.makeText(this, R.string.toast_no_tags_entered, Toast.LENGTH_SHORT).show();
		else
		{
			// Convert the tags to a String (so that they can be set as multiple choice items)
			// and create a boolean array used to keep track of which have been ticked by the user.
			String[] tagsString = new String[tags.size()];
			// Assigning the String values and then setting the boolean array if needed
			for (int i = 0; i < tags.size(); i++)
			{
				tagsString[i] = tags.get(i).getName();
			}

			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			FragmentManager manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_REMOVE_TAG_LIST) == null)
			{
				DialogFragment newFragment = TagRemoveDialogue.newInstance(this.walks, tagsString, tags);
				newFragment.show(manager, DIALOGUE_FRAGMENT_REMOVE_TAG_LIST);
			}
		}
	}

	/**
	 * Shows a list of all tags in a dialogue pop-up which enables the user to filter the walk list
	 * to only show walks containing at least one of the selected tags. Each tag is a separate
	 * check-box in a scrollable list.
	 */
	private void showTagList()
	{
		// Get a list of all tags. If there are no tags, show a Toast and end.
		final ArrayList<Tag> tags = DataSource.getAllTags();
		if (tags.size() == 0)
			Toast.makeText(this, R.string.toast_no_tags_entered, Toast.LENGTH_SHORT).show();
		else
		{
			// Convert the tags to a String (so that they can be set as multiple choice items)
			// and create a boolean array used to keep track of which have been ticked by the user.
			String[] tagsString = new String[tags.size()];
			final boolean[] checkedItems = new boolean[tags.size()];
			// Assigning the String values and then setting the boolean array if needed
			for (int i = 0; i < tags.size(); i++)
			{
				tagsString[i] = tags.get(i).getName();
				checkedItems[i] = false;

				// If tags are already checked (means some of the booleans should be ticked)
				if (this.checkedTags != null)
				{
					// Go through each checked tag - if it matches something in the CharSequence,
					// then the tag should already be ticked
					for (Tag checkedTag : this.checkedTags)
					{
						if (tagsString[i].equals(checkedTag.getName()))
						{
							checkedItems[i] = true;
						}
					}
				}
			}

			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			FragmentManager manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_TAGS) == null)
			{
				DialogFragment newFragment = TagListDialogue.newInstance(tagsString, tags, checkedItems);
				newFragment.show(manager, DIALOGUE_FRAGMENT_TAGS);
			}
		}
	}

	// ///////////////////////
	//
	// ListView manipulation methods
	//
	// ///////////////////////
	/**
	 * Updates the walks List by retrieving the current walks from the DataSource, sends them
	 * to the adapter, and informs the adapter that the DataSet has changed.
	 */
	@Override
	public void updateWalksList()
	{
		// If filtered by tags
		if (this.filteredByTags)
			filterWalksByTag(this.checkedTags);
		else
		{
			// Get the sort order from the preferences, defaulting to
			int sortValue = this.preferences.getInt(PreferenceActivity.SORT_LIST, DATE_DESCENDING);
			this.walks = DataSource.getAllWalks(sortValue);
			this.adapter.setWalks(this.walks);
			this.adapter.notifyDataSetChanged();
		}
	}

	/**
	 * Clears the tag filters, shows all Walks in the ListView and resets the options menu.
	 */
	private void displayAll()
	{
		this.filteredByTags = false;
		this.checkedTags = null;
		updateWalksList();
		invalidateOptionsMenu();
	}

	/**
	 * Filters walks from the ListView based on a supplied List of Tags.<br>
	 * <br>
	 * Saves the checked tags (so that they can be used in the future). If no tags were selected, shows
	 * a Toast for the user stating that. Otherwise, sets filteredByTags to true, gets the sort value,
	 * retrieves a complete list of walks, and creates an ArrayList containing a copy of those values.<br>
	 * <br>
	 * It then iterates through each walk, checking if the Walk contains any of the selected tags.
	 * If it does, it toggles a boolean and breaks. It then removes the walk if the boolean hasn't
	 * been toggled.
	 * 
	 * @param receivedCheckedTags <i>ArrayList<Tag></i>	The tags the user wishes to view.
	 */
	public void filterWalksByTag(ArrayList<Tag> receivedCheckedTags)
	{
		// Saving the checked tags for future use
		this.checkedTags = receivedCheckedTags;

		if (receivedCheckedTags.size() == 0)
		{
			Toast.makeText(this, R.string.toast_no_tags_selected, Toast.LENGTH_LONG).show();
			displayAll();
			return;
		}

		this.filteredByTags = true;

		int checkedItem = this.preferences.getInt(PreferenceActivity.SORT_LIST, CustomListActivity.DATE_DESCENDING);

		this.walks = DataSource.getAllWalks(checkedItem);
		ArrayList<Walk> allWalks = new ArrayList<Walk>(this.walks);

		// Iterate through each Walk - retrieves the tags in a List, and uses retainAll to see if the
		// walk contains any of the checked tags. If not, or if the walk has no tags, it's removed.
		for (Walk walk : allWalks)
		{
			if (walk.getTags() != null)
			{
				ArrayList<Tag> tagList = new ArrayList<Tag>(walk.getTags());
				tagList.retainAll(this.checkedTags);
				if (tagList.size() == 0)
				{
					this.walks.remove(walk);
				}
			}
			else
				this.walks.remove(walk);
		}
		
		// If no walks left (which implies it was already filtered and the user has deleted the last walk in the list)
		if (this.walks.size() == 0)
		{
			Toast.makeText(this, R.string.toast_no_tagged_items_remaining, Toast.LENGTH_LONG).show();
			displayAll();
		}
		else
		{
			this.adapter.setWalks(this.walks);
			this.adapter.notifyDataSetChanged();
			invalidateOptionsMenu();
		}
	}

	// ///////////////////////
	//
	// Dialogue-called methods
	//
	// ///////////////////////
	/**
	 * Opens the Android location settings - so that the user can enable GPS.<br>
	 * <br>
	 * Can be called by EnableGPSDalogue<br>
	 * <br>
	 * Sends a result to onActivityResult when the user returns from the settings page.
	 */
	public void openGPSSettings()
	{
		startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), WalkListActivity.GPS_ENABLED);
	}

	/**
	 * Cancels attempting to start a walk.<br>
	 * <br>
	 * Can be called by WaitingForGPSDialogue, StartNewWalkDialogue and GPSService.<br>
	 * <br>
	 * Tells the service that the "walk" is finished (so that it shuts down), dismisses either of the dialogues
	 * related if in the foreground unbinds from and stops the service, and sets all related booleans and objects
	 * to null.
	 */
	public void cancelGPS()
	{
		this.service.walkFinished();

		if (this.inForeground)
		{
			// Get the "New Walk" dialogue.
			DialogFragment newWalkDialogue = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOGUE_FRAGMENT_SHOW_NEW_WALK);
			if (newWalkDialogue != null)
			{
				newWalkDialogue.dismiss();
			}
			// "New Walk" is null, so gpsDialogue should be showing
			else
			{
				DialogFragment gpsDialogue = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOGUE_FRAGMENT_WAITING_FOR_GPS);
				if (gpsDialogue != null)
				{
					gpsDialogue.dismiss();
				}
			}
		}
		if (this.conn != null)
		{
			unbindService(this.conn);
		}
		stopService(this.serviceIntent);

		this.serviceRunning = false;
		this.trackingWalk = false;
		this.gpsLocked = false;
		this.conn = null;
		this.serviceIntent = null;
	}

	/**
	 * Returns the gpsLocked status.<br>
	 * <br>
	 * Can be called by WalkOnShowListener.<br>
	 * <br>
	 * Allows WalkOnShowListener find out if GPS is locked or not.
	 * 
	 * @return	Whether GPS is locked or not.
	 */
	public boolean getGPSLocked()
	{
		return this.gpsLocked;
	}

	/**
	 * Starts GPS tracking.<br>
	 * <br>
	 * Sets trackingWalk to true, and tells the service to start tracking walk points.
	 */
	public void startGPSTracking()
	{
		this.trackingWalk = true;
		this.service.startTrackingWalk();
	}

	// ///////////////////////
	//
	// Other methods
	//
	// ///////////////////////
	/**
	 * Sets the gpsLocked variable to true and if there's a "Waiting for GPS" dialogue, starts tracking.<br>
	 * <br>
	 * Can be called by GPSService.<br>
	 * <br>
	 * Sets gpsLocked to true. If a dialogue is open, dismisses it and, if the activity is in the foreground,
	 * opens MapWalkActivity.
	 */
	public void setGPSLocked()
	{
		this.gpsLocked = true;
		// gpsProgressDialogue (Waiting for GPS... dialogue) isn't null. Close the dialogue and start tracking.
		DialogFragment gpsDialogue = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOGUE_FRAGMENT_WAITING_FOR_GPS);
		if (gpsDialogue != null)
		{
			gpsDialogue.dismiss();
			
			startGPSTracking();

			// In foreground (so user hasn't browsed away). Launch the activity!
			if (this.isInForeground())
			{
				Walk walk = DataSource.getWalkById(0);
				Intent intent = new Intent(this, com.digitalpies.promenade.maps.MapWalkActivity.class);
				intent.putExtra(WALK_TAG, walk);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		}
	}

	/**
	 * Used if the user opts to enable GPS when prompted.<br>
	 * <br>
	 * If it has been enabled, shows the new walk" dialogue. Otherwise, shows the user a Toast saying
	 * that they cannot use the app without enabling GPS.<br>
	 * 
	 * @param requestCode	The requestCode that was sent.
	 * @param resultCode	The resultCode from the request
	 * @param data			Un-used
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// If the user was asked to enable GPS, checks if they did. If so, shows the new walk dialogue.
		// Otherwise, shows a toast.
		if (requestCode == GPS_ENABLED)
		{
			if (((LocationManager) getSystemService(Context.LOCATION_SERVICE))
					.isProviderEnabled(LocationManager.GPS_PROVIDER))
				showNewWalkDialogue();
			else
				Toast.makeText(this, R.string.toast_gps_wasnt_enabled, Toast.LENGTH_LONG).show();
		}
	}

	public boolean isInForeground()
	{
		return this.inForeground;
	}

	public void setInForeground(boolean inForeground)
	{
		this.inForeground = inForeground;
	}

	// ///////////////////////
	//
	// Inner classes
	//
	// ///////////////////////
	/**
	 * A custom ServiceConnection class that just gets a reference to the service, and gives
	 * the service a reference to it, to allow communication between the classes.
	 * 
	 * @author Alex Hardwicke
	 */
	private class MyServiceConnection implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder)
		{
			LocalBinder binder = (LocalBinder) iBinder;
			WalkListActivity.this.service = binder.getService();
			WalkListActivity.this.service.setWalkListActivity(WalkListActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
		}
	}
}