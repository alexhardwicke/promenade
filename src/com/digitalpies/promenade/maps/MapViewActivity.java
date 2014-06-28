package com.digitalpies.promenade.maps;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.dialogue.WalkDetailsDialogue;
import com.digitalpies.promenade.dialogue.WalkDialogue;
import com.digitalpies.promenade.walklist.CustomListActivity;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Shows the user the walk they have tapped in the main list. Extends CustomMapActivity for common map methods.<br>
 * <br>
 * Allows the user to view and delete photos and notes on the walk, or to edit the walk's details.<br>
 * <br>
 * The user can toggle the display of notes or photos via action bar buttons.
 * 
 * @author Alex Hardwicke
 */
public class MapViewActivity extends CustomMapActivity
{
	private static final String NOTES_HIDDEN = "NOTES_HIDDEN";
	private static final String PHOTOS_HIDDEN = "PHOTOS_HIDDEN";
	private static final String CONFIGURATION_CHANGE = "CONFIGURATION_CHANGE";

	private boolean photosHidden = false;
	private boolean notesHidden = false;
	private boolean hasPhotos = false;
	private boolean hasNotes = false;

	// ///////////////////////
	//
	// Initialisation methods
	//
	// ///////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Getting the geoPoints and setting them on the lineOverlay
		this.geoPoints = DataSource.getGeoPointsForWalk(this.walk);
		this.lineOverlay = new LineOverlay(this.geoPoints, this.mapView.getProjection());

		// Finding out if the walk contains photos and/or notes
		if (DataSource.getPhotoCountForWalk(this.walk) > 0) this.hasPhotos = true;
		if (DataSource.getNoteCountForWalk(this.walk) > 0) this.hasNotes = true;

		boolean configurationChange = false;

		// Restore the saved instance state
		if (savedInstanceState != null)
		{
			this.photosHidden = savedInstanceState.getBoolean(PHOTOS_HIDDEN);
			this.notesHidden = savedInstanceState.getBoolean(NOTES_HIDDEN);
			configurationChange = savedInstanceState.getBoolean(CONFIGURATION_CHANGE, false);
		}
		
		// Only centre & zoom if it's not being created by configuration change
		// (to prevent the camera moving to the start on rotate) and there is
		// at least one geoPoint (safety check)
		if (!configurationChange && this.geoPoints.size() != 0)
		{
			this.mapView.getController().animateTo(this.geoPoints.get(0));
			this.mapView.getController().setZoom(17);
		}

		drawOverlays();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		// Saving the user's photo/note hidden status so that they don't re-appear on rotate
		outState.putBoolean(PHOTOS_HIDDEN, this.photosHidden);
		outState.putBoolean(NOTES_HIDDEN, this.notesHidden);

		// If being destroyed due to a configuration change, put that into the out state
		if (isChangingConfigurations()) outState.putBoolean(CONFIGURATION_CHANGE, true);
	}

	// ///////////////////////
	//
	// Menu methods
	//
	// ///////////////////////
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_view_menu, menu);

		// If the walk has photos and photos are hidden, set the icon to highlighted
		if (this.hasPhotos)
		{
			if (this.photosHidden) menu.findItem(R.id.hide_photos_button).setIcon(R.drawable.ic_menu_gallery_highlighted);
		}
		// If the walk doesn't have photos, hide the button
		else
			menu.findItem(R.id.hide_photos_button).setVisible(false);

		// If the walk has notes and the notes are not hidden, set the icon to highlighted
		if (this.hasNotes)
		{
			if (this.notesHidden) menu.findItem(R.id.hide_notes_button).setIcon(R.drawable.ic_menu_pin_highlighted);
		}
		// If the walk doesn't have notes, hide the button
		else
			menu.findItem(R.id.hide_notes_button).setVisible(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		FragmentManager manager;
		Intent intent;
		switch (item.getItemId())
		{
		case android.R.id.home:
			// "Up" icon in top left pressed. Return to the Walk List.
			intent = new Intent(this, com.digitalpies.promenade.walklist.WalkListActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity(intent);
			return true;
		case R.id.edit_walk_button:
			// Edit walk has been pressed. Opens the edit menu from WalkListActivity.
			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_EDIT_WALK) == null)
			{
				DialogFragment newFragment = WalkDialogue.newInstance(CustomListActivity.EDIT_WALK_TASK, this.walk);
				newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_EDIT_WALK);
			}
			return true;
		case R.id.view_walk_details_button:
			// Show the view walk dialogue and break the loop.
			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
			// button)
			manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS) == null)
			{
				DialogFragment newFragment = WalkDetailsDialogue.newInstance(this.walk);
				newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_VIEW_WALK_DETAILS);
			}
			return true;
		case R.id.hide_photos_button:
			// Invert the hide photos boolean, reset the menu and map overlays
			this.photosHidden = !this.photosHidden;
			invalidateOptionsMenu();
			drawOverlays();
			return true;
		case R.id.hide_notes_button:
			// Invert the hide notes boolean, reset the menu and map overlays
			this.notesHidden = !this.notesHidden;
			invalidateOptionsMenu();
			drawOverlays();
			return true;
		case R.id.preferences_button:
			// Preferences button has been pressed. Launches the preferences activity
			intent = new Intent(this, com.digitalpies.promenade.PreferenceActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}

	// ///////////////////////
	//
	// Other methods
	//
	// ///////////////////////
	/**
	 * Draws the overlays into the map. Clears the map overlays, adds the line,
	 * adds the photos if photosHidden is false, adds the notes if notesHidden
	 * is false, and then invalidates the mapView to force a re-draw.
	 */
	@Override
	public void drawOverlays()
	{
		super.drawOverlays();

		if (!this.photosHidden) drawPhotoIcons();

		if (!this.notesHidden) drawNoteIcons();
		if (this.photoList.size() == 0 || this.noteList.size() == 0) invalidateOptionsMenu();

		this.mapView.invalidate();
	}

	/**
	 * Edits the provided walk by inserting the data into the database and updates the subtitle.
	 * 
	 * @param receivedWalk	The walk that has been edited
	 */
	public void editWalk(Walk receivedWalk)
	{
		// Insert the edited walk into the database and update the action bar title
		DataSource.editWalk(receivedWalk);
		getActionBar().setSubtitle(receivedWalk.getName());
	}
}