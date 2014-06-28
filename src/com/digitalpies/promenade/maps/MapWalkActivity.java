package com.digitalpies.promenade.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.dialogue.CancelWalkDialogue;
import com.digitalpies.promenade.dialogue.NoteDialogue;
import com.digitalpies.promenade.dialogue.WalkDialogue;
import com.digitalpies.promenade.gps.GPSService;
import com.digitalpies.promenade.gps.GPSService.LocalBinder;
import com.digitalpies.promenade.walklist.CustomListActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Shows the user the walk they are taking, and taken photos if any.<br>
 * <br>
 * Allows the user to take photos or create notes and add them to the walk, and to via the action bar, 
 * to complete or cancel the walk, to pause or resume the GPS tracking, and to toggle whether the camera
 * should focus each time the path is updated or not.
 * 
 * @author Alex Hardwicke
 */
public class MapWalkActivity extends CustomMapActivity
{
	public static final int TAKE_PHOTO = 0;
	public static final int ENABLE_GPS = 2;

	protected GPSService service;
	protected MapWalkActivity activity = this;

	private ImageView positionView;
	private MyServiceConnection conn;

	protected boolean paused;
	protected boolean serviceBound = false;

	private boolean cameraFound = false;
	private boolean focusEnabled = true;
	private int previousGeoPointSize = 0;

	// ///////////////////////
	//
	// Initialisation and status change methods
	//
	// ///////////////////////
	/**
	 * Checks if the system has a camera and sets cameraFound if true and binds itself to the service.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
		{
			this.cameraFound = true;
		}

		Intent intent = new Intent();
		intent.setClassName("com.digitalpies.promenade", "com.digitalpies.promenade.gps.GPSService");
		this.conn = new MyServiceConnection();
		bindService(intent, this.conn, Context.BIND_AUTO_CREATE);

		this.positionView = new ImageView(this);

		this.mapView.getController().setZoom(17);
	}

	/**
	 * If it's bound to a service, sets MapOpen to true, retrieves the GeoPoints, Photos and Notes,
	 * creates the LineOverlay and draws the overlays on the map.
	 */
	@Override
	public void onResume()
	{
		super.onResume();

		if (this.serviceBound)
		{
			this.service.setMapOpen(true);
			this.geoPoints = this.service.getGeoPoints();
			this.photoList = DataSource.getPhotosForWalk(this.walk);
			this.noteList = DataSource.getNotesForWalk(this.walk);

			this.lineOverlay = new LineOverlay(this.geoPoints, this.mapView.getProjection());
			drawOverlays();
		}
	}

	/**
	 * Tells the service that the map is not open any more.
	 */
	@Override
	public void onPause()
	{
		super.onPause();

		if (this.serviceBound) this.service.setMapOpen(false);
	}

	/**
	 * Unbinds itself from the service when being destroyed
	 */
	@Override
	public void onDestroy()
	{
		unbindService(this.conn);
		super.onDestroy();
	}

	// ///////////////////////
	//
	// Walk status methods
	//
	// ///////////////////////
	/**
	 * Run when the user has confirmed they wish to finish the recording of a walk.<br>
	 * <br>
	 * Saves the walk into the database and ends the walk.
	 */
	public void saveWalk(Walk receivedWalk)
	{
		DataSource.saveWalk(receivedWalk);
		endWalk();
	}

	/**
	 * Run when the user has confirmed they wish to cancel the recording of a walk.<br>
	 * <br>
	 * If selected is true, deletes all photos for the walk from the phone. Then cancels
	 * the walk from the database and ends the walk.
	 * 
	 * @param selected	Whether the user wishes to delete the walk's photos or not.
	 */
	public void cancelWalk(boolean selected)
	{
		if (selected)
		{
			for (int i = 0; i < this.photoList.size(); i++)
			{
				deletePhoto(i, true);
			}
		}

		DataSource.cancelWalk();
		endWalk();
	}

	/**
	 * Run when the user has ended a walk, whether saving or cancelling. Tells the service it's
	 * no longer needed, and opens a WalkListActivity instance with CLEAR_TOP and SINGLE_TOP
	 * flags to close all copies of MapWalkActivity. Then finishes the activity.
	 */
	public void endWalk()
	{
		this.service.walkFinished();

		// Launch a FLAG_ACTIVITY_CLEAR_TOP intent which will clear all copies of MapWalkActivity that are open.
		Intent intent = new Intent(this, com.digitalpies.promenade.walklist.WalkListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		finish();
	}

	// ///////////////////////
	//
	// "New" methods
	//
	// ///////////////////////
	/**
	 * Used by the service when there's a new GeoPoint for the walk.<br>
	 * <br>
	 * Adds the geoPoint to the list, re-creates the line overlay, and draws the overlays.
	 * @param geoPoint
	 */
	public void newPoint(GeoPoint geoPoint)
	{
		this.geoPoints.add(geoPoint);
		this.lineOverlay = new LineOverlay(this.geoPoints, this.mapView.getProjection());
		drawOverlays();
	}

	/**
	 * Used when the user has created a new note.
	 * 
	 * @param noteText	The text the user has entered.
	 */
	public void newNote(String noteText)
	{
		this.geoPoints = this.service.getGeoPoints();
		DataSource.createNote(0, this.geoPoints.get(this.geoPoints.size() - 1).getLatitudeE6() / 1E6,
				this.geoPoints.get(this.geoPoints.size() - 1).getLongitudeE6() / 1E6, noteText);
		this.noteList = DataSource.getNotesForWalk(this.walk);
		drawOverlays();
	}

	// ///////////////////////
	//
	// Draw methods
	//
	// ///////////////////////
	/**
	 * Draws the map overlays on to the map.<br>
	 * <br>
	 * Clears the current overlays, zooms to the final point if enabled and adds the line to the overlay. Then draws
	 * the photo and note icons, if any, sets the positionView background to the animated icon resource, gets
	 * the AnimationDrawable that has been set and starts the animation. It then adds positionView to the map,
	 * centered at the most recent geoPoint, and invalidates the mapView.
	 * 
	 */
	@Override
	public void drawOverlays()
	{
		// If an item dialogue is open, don't draw overlays.
		if (super.isItemTapped()) return;
		super.drawOverlays();

		// If focus is enabled and there's a new point
		if (this.focusEnabled && this.geoPoints.size() > this.previousGeoPointSize)
		{
			this.previousGeoPointSize = this.geoPoints.size();
			this.mapView.getController().animateTo(this.geoPoints.get(this.geoPoints.size() - 1));
		}

		drawPhotoIcons();
		drawNoteIcons();

		this.positionView.setBackgroundResource(R.drawable.animated_icon);
		AnimationDrawable frameAnimation = (AnimationDrawable) this.positionView.getBackground();

		frameAnimation.start();

		this.mapView.addView(this.positionView);

		MapView.LayoutParams layoutParams = new MapView.LayoutParams(MapView.LayoutParams.WRAP_CONTENT,
				MapView.LayoutParams.WRAP_CONTENT, this.geoPoints.get(this.geoPoints.size() - 1),
				MapView.LayoutParams.CENTER);
		this.positionView.setLayoutParams(layoutParams);

		this.mapView.invalidate();
	}

	// ///////////////////////
	//
	// Menu methods
	//
	// ///////////////////////
	/**
	 * Creates the options menu, showing either the pause or resume button, the focus enable or
	 * disable button, and the camera button only if the device has a camera.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_walk_menu, menu);
		menu.findItem(R.id.pause_button).setVisible(!this.paused);
		menu.findItem(R.id.resume_button).setVisible(this.paused);
		menu.findItem(R.id.take_photo_button).setVisible(this.cameraFound);
		menu.findItem(R.id.walk_zoom_enable_camera_button).setVisible(!this.focusEnabled);
		menu.findItem(R.id.walk_zoom_disable_camera_button).setVisible(this.focusEnabled);
		return true;
	}

	/**
	 * Handles actionbar buttons being pressed.<br>
	 * <br>
	 * If the home button has been pressed, goes back to WalkListActivity.<br>
	 * If the photo button has been pressed, makes sure the folder where things will be saved exists.
	 * If it doesn't, creates it. Checks if a file called "temp.jpg" exists - if so, deletes it. Then
	 * launches the Camera app, showing a Toast for the user if a ActivityNotFoundException is thrown.<br>
	 * If the take note button is pushed, opens a "new note" dialogue.<br>
	 * If the pause button is pushed, pauses the service and invalidates the menu.<br>
	 * If the resume button is pushed, resumes the service and invalidates the menu.<br>
	 * If the focus enable button is pushed, sets the focusEnabled boolean to true so that the map
	 * auto focuses when a new point is saved.<br>
	 * If the focus disable button is pushed, sets the focusEnabled boolean to false so that the map
	 * doesn't move when a new point is saved.<br>
	 * If the cancel button is pushed, the user is shown a dialogue asking if they are sure.<br>
	 * If the finish button is pushed, the user is given the opportunity to update the walk details
	 * before it saves, or to cancel the dialogue and continue on the walk.<br>
	 * If the preferences option is chosen, opens the application preferences.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		DialogFragment newFragment;
		FragmentManager manager;
		File folder, file;
		boolean result;

		switch (item.getItemId())
		{
		case android.R.id.home:
			// "Up" icon in top left pressed. Return to the Walk List.
			intent = new Intent(this, com.digitalpies.promenade.walklist.WalkListActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.take_photo_button:
			// Camera button has been pressed. Sends an IMAGE_CAPTURE intent to the OS to try to get a photo.
			// First make sure we can write to the SD card.
			// If it's NOT mounted normally (i.e. with R/W access), show a toast and return
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
			{
				Toast.makeText(this, R.string.toast_storage_error, Toast.LENGTH_LONG).show();
				return true;
			}

			// SD card can be accessed. Make sure the folder exists
			folder = new File(Environment.getExternalStorageDirectory().toString() + "/PhotoQuest");
			result = folder.exists();
			if (!result)
			{
				// If it doesn't, create it
				folder.mkdirs();
			}
			// Check if there's a "temp.jpg". If so, delete it.
			file = new File(folder, "/temp.jpg");
			if (file.exists()) file.delete();
			intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

			// Create a URI for the file and pass it to the camera intent
			Uri imageUri = Uri.fromFile(file);
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
			try
			{
				startActivityForResult(intent, TAKE_PHOTO);
			}
			catch (ActivityNotFoundException e)
			{
				Toast.makeText(this, R.string.no_camera_app_installed, Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.take_note_button:
			// Note button has been pressed. Allow the user to enter a note.
			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_ADD_NOTE) == null)
			{
				newFragment = NoteDialogue.newInstance();
				newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_ADD_NOTE);
			}
			return true;
		case R.id.pause_button:
			this.paused = true;
			this.service.pause();
			invalidateOptionsMenu();
			break;
		case R.id.resume_button:
			this.paused = false;
			this.service.resume();
			invalidateOptionsMenu();
			break;
		case R.id.walk_zoom_enable_camera_button:
			this.focusEnabled = true;
			invalidateOptionsMenu();
			break;
		case R.id.walk_zoom_disable_camera_button:
			this.focusEnabled = false;
			invalidateOptionsMenu();
			break;
		case R.id.walk_cancel_button:
			// User is cancelling the walk. Shows a confirmation dialogue, and if confirmed, deletes all walk data and ends
			// the activity.
			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_CANCEL_WALK) == null)
			{
				newFragment = CancelWalkDialogue.newInstance((this.photoList.size() > 0));
				newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_CANCEL_WALK);
			}
			return true;
		case R.id.walk_finished_button:
			// User is finished with the walk. Shows a confirmation dialogue, and if confirmed, allows the user to edit the
			// walk details before everything is saved to the database.
			// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
			manager = getFragmentManager();
			if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_SAVE_WALK) == null)
			{
				newFragment = WalkDialogue.newInstance(SAVE_WALK_TASK, this.walk);
				newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_SAVE_WALK);
			}
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
	 * Run when the user returns from taking a photo.<br>
	 * <br>
	 * If the resultCode is that it was cancelled, ends.<br>
	 * <br>
	 * Else, if the requestCode was TAKE_PHOTO, then it retrieves the photo file, creates the final
	 * folder path and filename for it (In the form PyyyyMMddhhmmss.jpg) and renames the photo.<br>
	 * <br>
	 * It then provides additional EXIF data to the photo file in the form of GPS positions, and
	 * finally, inserts the photo into the database.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_CANCELED) return;

		// Photo retrieved from camera.
		if (requestCode == TAKE_PHOTO)
		{
			try
			{
				// Retrieves the temp photo location
				File photoOrigin = new File(Environment.getExternalStorageDirectory(), "/PhotoQuest/temp.jpg");

				// Creating the new path for the file
				String folderPath = Environment.getExternalStorageDirectory().toString() + "/PhotoQuest";
				File folder = new File(folderPath);

				// Creating the new filename
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
				String date = dateFormat.format(new Date());
				String fileString = "P" + date + ".jpg";
				final File newPath = new File(folder.toString(), fileString);

				// Renaming the photo
				photoOrigin.renameTo(newPath);

				// Want to set the EXIF data (location) for the new photo
				ExifInterface exif = new ExifInterface(newPath.getAbsolutePath());

				// Getting GPS coordinate. If geoPoints is empty (happens rarely if the user tries to
				// take a photo the instant the activity opens), update it.
				if (this.geoPoints.size() == 0) this.geoPoints = this.service.getGeoPoints();

				double latitude = this.geoPoints.get(this.geoPoints.size() - 1).getLatitudeE6() / 1E6;
				double longitude = this.geoPoints.get(this.geoPoints.size() - 1).getLongitudeE6() / 1E6;

				// Setting lat
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDMS(latitude));
				if (latitude > 0)
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
				else
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");

				// Setting lon
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDMS(longitude));
				if (longitude > 0)
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
				else
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");

				// Saving the new attributes
				exif.saveAttributes();

				// Insert the photo into the database
				DataSource.createPhoto(0, latitude, longitude, newPath.getAbsolutePath());
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * Converts a decimal GeoPoint value to a String containing it in DMS form.<br>
	 * <br>
	 * Algorithm and code from the Wikipedia:<br>
	 * https://en.wikipedia.org/wiki/Geographic_coordinate_conversion#Java_Implementation
	 * 
	 * @param coord	The coordinate to be converted
	 * 
	 * @return		The converted coordinate in DMS form
	 */
	public static String decimalToDMS(double coord)
	{
		String out, deg, min, sec;
		double innerCoord = Math.abs(coord);

		double mod = innerCoord % 1;
		int intPart = (int) innerCoord;

		deg = String.valueOf(intPart);

		innerCoord = mod * 60;
		mod = innerCoord % 1;
		intPart = (int) innerCoord;

		min = String.valueOf(intPart);

		innerCoord = mod * 60;
		intPart = (int) innerCoord;

		sec = String.valueOf(intPart);

		out = deg + "/1," + min + "/1," + sec + "/1";

		return out;
	}

	// ///////////////////////
	//
	// Inner classes
	//
	// ///////////////////////
	/**
	 * A custom ServiceConnection class that retrieves a reference to the service, provides the
	 * service with a reference to the current activity, sets serviceBound to true, tells the
	 * service that the map is open, gets the service paused status and updates the menu.<br>
	 * <br>
	 * Then retrieves the GeoPoints from the service, and the notes and photos from the database,
	 * creates the lineOverlay, and draws the overlays on the map.
	 * 
	 * @author Alex Hardwicke
	 */
	private class MyServiceConnection implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder)
		{
			LocalBinder binder = (LocalBinder) iBinder;
			MapWalkActivity.this.service = binder.getService();
			MapWalkActivity.this.service.setMapWalkActivity(MapWalkActivity.this.activity);
			MapWalkActivity.this.serviceBound = true;

			MapWalkActivity.this.service.setMapOpen(true);

			MapWalkActivity.this.paused = MapWalkActivity.this.service.getPaused();
			invalidateOptionsMenu();

			MapWalkActivity.this.geoPoints = MapWalkActivity.this.service.getGeoPoints();
			MapWalkActivity.this.photoList = DataSource.getPhotosForWalk(MapWalkActivity.this.walk);
			MapWalkActivity.this.noteList = DataSource.getNotesForWalk(MapWalkActivity.this.walk);
			MapWalkActivity.this.lineOverlay = new LineOverlay(MapWalkActivity.this.geoPoints,
					MapWalkActivity.this.mapView.getProjection());
			drawOverlays();
		}

		@Override
		public void onServiceDisconnected(ComponentName className)
		{
		}
	}
}