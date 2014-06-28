package com.digitalpies.promenade.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Note;
import com.digitalpies.promenade.database.Photo;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.dialogue.DeleteNoteDialogue;
import com.digitalpies.promenade.dialogue.DeletePhotoDialogue;
import com.digitalpies.promenade.dialogue.NoteDialogue;
import com.digitalpies.promenade.walklist.CustomListActivity;
import com.digitalpies.promenade.walklist.WalkListActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

/**
 * An abstract implementation of MapActivity. Extended by both MapViewActivity and MapWalkActivity.<br>
 * <br>
 * Contains all of the methods that both classes use: drawing icons, updating and deleting notes, deleting
 * photos, adding custom zoom buttons onto the map, and has abstract methods for drawing
 * overlays and walks so that calls can be made regardless of which MapActivity is running.
 * 
 * @author Alex Hardwicke
 */
public abstract class CustomMapActivity extends MapActivity
{
	public static final String SAVE_WALK_TASK = "SAVE_WALK_TASK";

	private static final int DRAW_PHOTO = 1;
	private static final int DRAW_STACKED_PHOTO = 3;
	private static final int DRAW_NONE = 6;
	private static final int GRID_SIZE = 25;

	public static final String STACKED_PHOTO = "STACKED_PHOTO";
	public static final String PHOTO = "PHOTO";

	protected CustomMapView mapView;
	protected ArrayList<Photo> photoList;
	protected ArrayList<Note> noteList;
	protected PhotoItemizedOverlay photoOverlay;
	protected PhotoItemizedOverlay photoStackedOverlay;
	protected NoteItemizedOverlay noteOverlay;
	protected Drawable photoDrawable;
	protected Drawable photoStackedDrawable;
	protected Drawable noteDrawable;
	protected LineOverlay lineOverlay;
	protected Walk walk;
	protected List<Overlay> mapOverlays;
	protected ArrayList<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

	private ActionMode actionMode;
	private CustomActionMode customActionMode;

	private AsyncTask<Integer, Integer, Integer> animTask = null;
	private ArrayList<AsyncTask<Integer, Integer, Bitmap>> bitmapWorkerTasks;

	private boolean itemTapped = false;

	// ///////////////////////
	//
	// Initialisation, pause and resume methods
	//
	// ///////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mapwalk);

		// Make sure the DataSource is opened
		DataSource.openDataSource(this);

		// Getting the walk and setting up the Action Bar icon & title to the walk name
		this.walk = (Walk) getIntent().getExtras().getParcelable(WalkListActivity.WALK_TAG);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setSubtitle(this.walk.getName().toString());

		// Setting the ActionBar backgrounds
		BitmapDrawable bitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.stripes);
		bitmap.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		getActionBar().setBackgroundDrawable(bitmap);
		getActionBar().setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.purple_dot));

		// Setting up the mapview and overlays
		this.mapView = (CustomMapView) findViewById(R.id.map_view);

		this.mapOverlays = this.mapView.getOverlays();

		this.photoList = DataSource.getPhotosForWalk(this.walk);
		this.noteList = DataSource.getNotesForWalk(this.walk);

		// Two overlays for photos - one for the "stacked" drawable
		this.photoDrawable = getResources().getDrawable(R.drawable.ic_map_overlay_photo);
		this.photoOverlay = new PhotoItemizedOverlay(this.photoDrawable, this, this.mapView, PHOTO);

		this.photoStackedDrawable = getResources().getDrawable(R.drawable.ic_map_overlay_photo_stacked);
		this.photoStackedOverlay = new PhotoItemizedOverlay(this.photoStackedDrawable, this, this.mapView,
				STACKED_PHOTO);

		// Overlay for notes
		this.noteDrawable = getResources().getDrawable(R.drawable.ic_menu_pin_holo_light);
		this.noteOverlay = new NoteItemizedOverlay(this.noteDrawable, this, this.mapView);

		this.bitmapWorkerTasks = new ArrayList<AsyncTask<Integer, Integer, Bitmap>>();

		// Setting up the custom zoom buttons
		initialiseZoomButtons();
	}

	/**
	 * If there's an animTask that's still active (used in photo balloons to show the "loading" animation,
	 * cancel it.)<br>
	 * <br>
	 * If there are items inside the bitmapWorkerTasks ArrayList, iterate through them all and cancel them.<br>
	 * <br>
	 * Set itemTapped to false.
	 */
	@Override
	public void onDestroy()
	{
		if (this.animTask != null)
		{
			this.animTask.cancel(true);
			this.animTask = null;
		}

		if (this.bitmapWorkerTasks.size() > 0)
		{
			for (AsyncTask<Integer, Integer, Bitmap> asyncTask : this.bitmapWorkerTasks)
				asyncTask.cancel(false);
		}

		this.itemTapped = false;

		super.onDestroy();
	}

	/**
	 * Draws two ImageViews on the lower right of the map - for zooming. The standard MapView
	 * icons have not been updated for a long time and are completely out of place in an ICS
	 * application (Google's Maps application uses custom icons).<br>
	 * <br>
	 * The two ImageViews are retrieved from the layout, and have onTouch and onClick listeners
	 * set. The logic for the two is very similar:<br>
	 * <br>
	 * When a button is touched, if the app is fully zoomed in (for the zoom in button) or vice versa,
	 * it returns true so that the touch is dismissed. <br>
	 * If it isn't fully zoomed in, the touch action is checked. If it's a DOWN action, then the icon
	 * is changed to be highlighted (has a blue background). If it's a CANCEL or UP action, then the
	 * icon is set to the default.<br>
	 * <br>
	 * If the user clicks an icon (i.e. lets go of their finger while on the icon), it zooms in or out
	 * as appropriate.
	 */
	private void initialiseZoomButtons()
	{
		final ImageView zoomIn = (ImageView) findViewById(R.id.maps_zoom_in);
		final ImageView zoomOut = (ImageView) findViewById(R.id.maps_zoom_out);

		zoomIn.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// If fully zoomed in, don't change the image. Return!
				if (CustomMapActivity.this.mapView.getZoomLevel() == CustomMapActivity.this.mapView
						.getMaxZoomLevel()) return true;

				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN)
				{
					zoomIn.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_in_focused));
				}
				else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
				{
					zoomIn.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_in));
				}
				return false;
			}
		});
		zoomIn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				// Set itemTapped to false (as it will not be shown post-zoom)
				CustomMapActivity.this.itemTapped = false;
				// Hide the "item selected" action bar if showing
				if (CustomMapActivity.this.actionMode != null) CustomMapActivity.this.actionMode.finish();
				// Zoom in
				CustomMapActivity.this.mapView.getController().zoomIn();
			}
		});

		zoomOut.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				// If fully zoomed out, don't change the image. Return!
				if (CustomMapActivity.this.mapView.getZoomLevel() == 1) return true;

				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN)
				{
					zoomOut.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_out_focused));
				}
				else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP)
				{
					zoomOut.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_out));
				}
				return false;
			}
		});
		zoomOut.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				// Set itemTapped to false (as it will not be shown post-zoom)
				CustomMapActivity.this.itemTapped = false;
				// Hide the "item selected" action bar if showing
				if (CustomMapActivity.this.actionMode != null) CustomMapActivity.this.actionMode.finish();
				// Zoom out
				CustomMapActivity.this.mapView.getController().zoomOut();
			}
		});
		this.mapView.setUp(this, zoomIn, zoomOut);
	}

	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}

	// ///////////////////////
	//
	// Draw methods
	//
	// ///////////////////////
	/**
	 * Clears the noteOverlay, adds each note from the walk to the list,
	 * and then adds it to the MapOverlay.
	 */
	protected void drawNoteIcons()
	{
		this.noteOverlay.clear();
		for (int i = 0; i < this.noteList.size(); i++)
		{
			Note note = this.noteList.get(i);
			OverlayItem overlayItem = new OverlayItem(note.getGeoPoint(), i + "", note.getNote());
			this.noteOverlay.addOverlay(overlayItem);
		}
		this.mapOverlays.add(this.noteOverlay);
	}

	/**
	 * Clears the photoOverlays, calculates which photos should be hidden if any, and
	 * adds the appropriate photos to the appropriate overlays.
	 */
	protected void drawPhotoIcons()
	{
		// Clear the overlays
		this.photoOverlay.clear();
		this.photoStackedOverlay.clear();

		// Get the screen density
		double density = this.getResources().getDisplayMetrics().density;

		// A balloon array representing how each photo should be drawn.
		// DRAW_NORMAL means it should be drawn in as a normal photo,
		// DRAW_STACKED means it should have the multi-photo stacked icon, and
		// DRAW_NONE means it shouldn't be drawn at all.
		int[] drawPhotoStatus = new int[this.photoList.size()];

		// for (int i = 0; i < photoGeoPoints.size(); i++)
		for (int i = 0; i < this.photoList.size(); i++)
		{
			drawPhotoStatus[i] = DRAW_PHOTO;
		}

		// Get the grid size in pixels
		int gridSizeInPixels = (int) (GRID_SIZE * density + 0.5f);
		Projection projection = this.mapView.getProjection();

		// Iterate through all photos in two iterations. The outer iteration goes through all items
		// except for the last one, and the inner starts from the current outer item + 1 through
		// to the very last item. It uses the mapView projection to get the pixel locations of
		// both geoPoints that are being checked, and then compares the values. If the items
		// overlap, the outer item should be drawn stacked, and the inner item should be hidden
		// (giving the effect of merged icons)
		for (int i = 0; i < this.photoList.size() - 1; i++)
		{
			if (drawPhotoStatus[i] == 3) continue;
			GeoPoint geoPoint1 = this.photoList.get(i).getGeoPoint();
			Point outerPoint = projection.toPixels(geoPoint1, null);
			for (int j = i + 1; j < this.photoList.size(); j++)
			{
				if (drawPhotoStatus[j] == 3) continue;
				GeoPoint geoPoint2 = this.photoList.get(j).getGeoPoint();
				Point innerPoint = projection.toPixels(geoPoint2, null);
				if (outerPoint.x <= innerPoint.x + gridSizeInPixels
						&& outerPoint.x >= innerPoint.x - gridSizeInPixels
						&& outerPoint.y <= innerPoint.y + gridSizeInPixels
						&& outerPoint.y >= innerPoint.y - gridSizeInPixels)
				{
					drawPhotoStatus[i] = DRAW_STACKED_PHOTO;
					drawPhotoStatus[j] = DRAW_NONE;
				}
			}
		}

		// Iterate through the photoStatus array, and switch the value. If it's DRAW_NORMAL,
		// add it to photoOverlay. If it's DRAW_STACKED, add it to the photoStackedOverlay.
		for (int i = 0; i < drawPhotoStatus.length; i++)
		{
			switch (drawPhotoStatus[i])
			{
			case DRAW_PHOTO:
				this.photoOverlay.addOverlay(new OverlayItem(this.photoList.get(i).getGeoPoint(), i + "", null));
				break;
			case DRAW_STACKED_PHOTO:
				this.photoStackedOverlay.addOverlay(new OverlayItem(this.photoList.get(i).getGeoPoint(), i + "",
						null));
				break;
			}
		}

		// Add both overlays to mapOverlays.
		this.mapOverlays.add(this.photoOverlay);
		this.mapOverlays.add(this.photoStackedOverlay);
	}

	/**
	 * Base drawOverlays method called by both classes. Clears the mapOverlays and adds
	 * the line to the overlay list.
	 */
	protected void drawOverlays()
	{
		this.mapOverlays.clear();
		this.mapOverlays.add(this.lineOverlay);
	}

	// ///////////////////////
	//
	// Get/Set/Update/Delete methods
	//
	// ///////////////////////
	/**
	 * Returns the list of photos for the current walk.<br>
	 * 
	 * @return	The list of photos for the opened walk
	 */
	public ArrayList<Photo> getPhotos()
	{
		return this.photoList;
	}

	/**
	 * Returns the list of notes for the current walk.<br>
	 * 
	 * @return	The list of notes for the opened walk
	 */
	public ArrayList<Note> getNotes()
	{
		return this.noteList;
	}

	/**
	 * Updates the note for which the id is provided with the provided String.<br>
	 * <br>
	 * Used by NoteOnShowListener when editing a walk.
	 * 
	 * @param id	The note's ID.
	 * @param note	The new note text.
	 */
	public void updateNote(long id, String note)
	{
		DataSource.editNote(id, note);
		this.noteList = DataSource.getNotesForWalk(this.walk);
		drawOverlays();
	}

	/**
	 * Deletes the note for which the position is provided.<br>
	 * <br>
	 * Used by DeleteNoteDialogue when deleting a note.<br>
	 * 
	 * @param position	The note's position.
	 */
	public void deleteNote(int position)
	{
		long id = this.noteList.get(position).getId();
		DataSource.deleteNote(id);
		this.noteList = DataSource.getNotesForWalk(this.walk);
		drawOverlays();
	}

	/**
	 * Deletes the photo for which the details are provided.<br>
	 * <br>
	 * A boolean is provided - if true, the user as opted to also delete the photo from the phone's
	 * internal storage. It creates a File object using the fileText, and attempts to delete the file.
	 * If it fails, it shows an error toast for the user.<br>
	 * <br>
	 * Regardless of the boolean status, the photo is removed from the database, the photoList is
	 * updated, and the overlays are redrawn.
	 * 
	 * @param position	The position in the photoList of the photo
	 * @param selected	Whether the user wants the photo deleted from the phone as well.
	 */
	public void deletePhoto(int position, boolean selected)
	{
		Photo photo = this.photoList.get(position);
		if (selected)
		{
			File file = new File(photo.getFile());
			boolean result = file.delete();
			if (!result) Toast.makeText(this, R.string.error_deleting_photo, Toast.LENGTH_SHORT).show();
		}
		DataSource.deletePhoto(photo.getId());
		this.photoList = DataSource.getPhotosForWalk(this.walk);
		drawOverlays();
	}

	// ///////////////////////
	//
	// Balloon and ActionMode methods
	//
	// ///////////////////////
	/**
	 * Run whenever a note or photo overlay is tapped (and a balloon is created).
	 * If there's currently an ActionMode open (and as such, a balloon - the user
	 * is tapping from one to another), finish the current one and open a new
	 * one for the new item.
	 * 
	 * @param index				The index of the photo/note in photoList or noteList
	 * @param itemizedOverlay	The overlay that has been tapped
	 */
	public void itemTapped(int index, boolean isPhoto)
	{
		if (this.actionMode != null) this.actionMode.finish();
		this.itemTapped = true;
		this.customActionMode = new CustomActionMode(index, isPhoto);
		this.actionMode = startActionMode(this.customActionMode);
	}

	/**
	 * Used to set the selected item (used when the pager is swiped). Sets the index to the current item.
	 * 
	 * @param position	The position in the noteList or PhotoList of the item the user is viewing
	 */
	public void setSelected(int position)
	{
		if (this.customActionMode != null) this.customActionMode.setIndex(position);
	}

	/**
	 * Used to give the activity a reference to the animTask - so that, if the balloon containing
	 * the animTask is closed before the photo(s) have loaded and there's no need for it, it can
	 * be killed (as there is no event fired when a balloon is closed). If there's already an
	 * animTask, cancels it (as otherwise one will leak).
	 * 
	 * @param animTask	The animation task
	 */
	public void setAnimTask(AsyncTask<Integer, Integer, Integer> animTask)
	{
		if (this.animTask != null) this.animTask.cancel(true);
		this.animTask = animTask;
	}

	/**
	 * Used to give the activity a reference to each BitmapWorkerTask - so that all of these
	 * tasks can be killed when action mode ends or the activity is destroyed.<br>
	 * <br>
	 * This prevents one bug: without this, if the user rapidly taps an overlay item, a large
	 * amount of balloons are created and destroyed - but the bitmap tasks continue in serial
	 * mode regardless, which takes the final bitmap tasks a long time to finish. This way,
	 * all bitmap tasks processing for any specific balloon instance can be killed when the
	 * balloon is dismissed or the activity destroyed.
	 * 
	 * @param bitmapWorkerTask	The bitmap worker task to add.
	 */
	public void addBitmapWorkerTask(AsyncTask<Integer, Integer, Bitmap> bitmapWorkerTask)
	{
		this.bitmapWorkerTasks.add(bitmapWorkerTask);
	}

	/**
	 * Returns a balloon is open (if an item has been tapped) or not.
	 * 
	 * @return	True if an item has been tapped.
	 */
	public boolean isItemTapped()
	{
		return this.itemTapped;
	}

	/**
	 * Moves the camera position to where the photo the user is viewing is located.
	 * 
	 * @param position	The position in the photoList of the selected photo
	 */
	public void showItemOnMap(int position, boolean photo)
	{
		if (photo)
			this.mapView.getController().animateTo(this.photoList.get(position).getGeoPoint());
		else
			this.mapView.getController().animateTo(this.noteList.get(position).getGeoPoint());
	}

	// ///////////////////////
	//
	// Inner classes
	//
	// ///////////////////////
	/**
	 * A custom implementation of ActionMode.<br>
	 * <br>
	 * When created, is provided with the index of the item (in noteList or photoList), and a
	 * boolean informing the action mode whether the item that has been tapped is a photo or not.<br>
	 * <br>
	 * Expands a menu, always showing a "delete" button and a "zoom to" . If the item is a note, it
	 * shows an edit button, if a photo, a share button.<br>
	 * <br>
	 * When any button is sent, it opens the appropriate dialogue for edit/delete (if any), passing
	 * the index value to inform the dialogue which photo/note it should handle, runs the zoom
	 * method if the zoom button has been pressed, or fires off a share Intent if share has. It
	 * then finishes the action mode in all four cases, which in turn dismisses the balloon.<br>
	 * <br>
	 * It can also have the index updated, which happens every time the pager is swiped by the user.
	 * 
	 * @author Alex Hardwicke
	 */
	private class CustomActionMode implements ActionMode.Callback
	{
		private int index;
		private boolean photo = false;

		/**
		 * Constructor - takes in the index, and whether it's a photo or not
		 */
		public CustomActionMode(int index, boolean isPhoto)
		{
			this.index = index;
			this.photo = isPhoto;
		}

		/**
		 * Used when the pager is swiped. Keeps the actionMode up to date with which note is selected.
		 */
		public void setIndex(int position)
		{
			this.index = position;
		}

		/**
		 * Run when a menu button is pressed.<br>
		 * <br>
		 * If the edit button is pressed, runs a NoteDialogue sending the note's ID and note
		 * text (so that the dialogue is pre-populated).<br>
		 * If the zoom_to_photo button is pressed, it runs showItemOnMap for the current item.<br>
		 * If the delete button is pressed, it opens a DeletePhotoDialoge or DeleteNoteDialogue
		 * depending upon whether the item is a photo or not, passing the index to the dialogue.<br>
		 * <br>
		 * In all three cases, it finishes the action mode.
		 */
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			FragmentManager manager;
			// Get the checked items
			switch (item.getItemId())
			{
			case R.id.edit_button:
				// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
				// button)
				manager = getFragmentManager();
				if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_EDIT_NOTE) == null)
				{
					Note note = CustomMapActivity.this.noteList.get(this.index);
					DialogFragment newFragment = NoteDialogue.newInstance(note.getId(), note.getNote());
					newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_EDIT_NOTE);
				}
				mode.finish();
				return true;
			case R.id.share_button:
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("image/jpeg");
				File file = new File(CustomMapActivity.this.photoList.get(this.index).getFile());
				if (file.exists())
				{
					Uri uri = Uri.fromFile(new File(CustomMapActivity.this.photoList.get(this.index).getFile()));
					share.putExtra(Intent.EXTRA_STREAM, uri);
					startActivity(Intent.createChooser(share, getString(R.string.share)));
				}
				else
				{
					Toast.makeText(CustomMapActivity.this, R.string.toast_photo_error, Toast.LENGTH_LONG).show();
				}
				return true;
			case R.id.zoom_to_photo_button:
				showItemOnMap(this.index, this.photo);
				mode.finish();
				return true;
			case R.id.delete_button:
				if (this.photo)
				{
					// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
					// button)
					manager = getFragmentManager();
					if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_DELETE_PHOTO) == null)
					{
						DialogFragment newFragment = DeletePhotoDialogue.newInstance(this.index);
						newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_DELETE_PHOTO);
					}
				}
				else
				{
					// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
					// button)
					manager = getFragmentManager();
					if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_DELETE_PHOTO) == null)
					{
						DialogFragment newFragment = DeleteNoteDialogue.newInstance(this.index);
						newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_DELETE_PHOTO);
					}
				}
				mode.finish();
				return true;
			}
			return false;
		}

		/**
		 * Inflates the menu when created.
		 */
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			// Inflate the menu
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.map_contextual_menu, menu);
			return true;
		}

		/**
		 * When the action mode is destroyed, sets itemTapped to false, cancels the
		 * animTask when if it's still running, iterates through the bitmap tasks, cancelling them
		 * if possible and clearing the ArrayList, draws the overlays (which also dismisses the
		 * balloon), and nulls the activity reference to the actionMode.
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			CustomMapActivity.this.itemTapped = false;
			if (CustomMapActivity.this.animTask != null)
			{
				CustomMapActivity.this.animTask.cancel(true);
				CustomMapActivity.this.animTask = null;
			}

			if (CustomMapActivity.this.bitmapWorkerTasks.size() > 0)
			{
				for (AsyncTask<Integer, Integer, Bitmap> asyncTask : CustomMapActivity.this.bitmapWorkerTasks)
					asyncTask.cancel(false);
			}
			CustomMapActivity.this.bitmapWorkerTasks = new ArrayList<AsyncTask<Integer, Integer, Bitmap>>();

			drawOverlays();
			CustomMapActivity.this.actionMode = null;
		}

		/**
		 * Run when preparing the menu. Updates the title to show which type of item is selected,
		 * If a photo, also disables the edit note button.
		 */
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			if (this.photo)
			{
				mode.setTitle(getString(R.string.photo_selected));
				menu.getItem(0).setVisible(false);
			}
			else
			{
				menu.getItem(1).setVisible(false);
				mode.setTitle(getString(R.string.note_selected));
			}
			return true;
		}
	}
}
