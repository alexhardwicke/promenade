package com.digitalpies.promenade.gps;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.maps.MapWalkActivity;
import com.digitalpies.promenade.walklist.WalkListActivity;
import com.google.android.maps.GeoPoint;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * This class is a background service. It is used to handle GPS data when a walk is in progress.
 * When the service is created, it starts trying to get a GPS lock. Once this is done, it informs
 * the application that GPS has locked on - this is used to allow the application to progress past
 * the "Start Walk" dialogue (or close a Progress dialogue if the user has already clicked start).<br>
 * <br>
 * When GPS is locked and the user has clicked start, the service starts tracking the walk. When the
 * LocationListener receives a new Location update, it checks if it's time to update the tracking data - 
 * this is set by a simple boolean that toggles on every x seconds (customisable in user preferences).<br>
 * <br>
 * The GPS data is used to create a GeoPoint which is sent on the the application, and if the Map activity
 * is open, to be drawn on the map. The data is also put into the database with a walk ID of 0 (which is
 * reserved for the current walk in progress).<br>
 * <br>
 * When the walk has been finished (either saved or cancelled), or the user has cancelled starting a walk,
 * then all data is cleared, and the service is ended.
 * 
 * @author Alex Hardwicke
 */
public class GPSService extends Service
{
	private static final int NOTIFICATION_INT = 1;
	private static final int DISMISSABLE_NOTIFICATION_INT = 2;

	private static final String MAP_ACCURACY = "map_accuracy";

	public static boolean isRunning = false;
	public static boolean isCancelled = false;
	public static boolean trackingWalk = false;

	protected boolean gpsLocked = false;
	protected boolean readyForUpdate = false;
	protected boolean mapOpen = false;
	private boolean paused = false;
	private boolean resumePressed = false;

	protected ArrayList<GeoPoint> geoPoints;
	protected MapWalkActivity mapWalkActivity;
	protected WalkListActivity walkListActivity;

	private CustomLocationListener locationListener;
	private LocationManager locationManager;
	private Thread timer;
	private Notification trackingNotification;
	private Notification pausedNotification;
	private Notification waitingForGPSNotification;
	private final IBinder binder = new LocalBinder();

	/////////////////////////
	//
	// Initialisation and status change methods
	//
	/////////////////////////
	/**
	 * Run when the service is bound - returns a bound messenger.
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		isRunning = true;
		return this.binder;
	}

	public class LocalBinder extends Binder
	{
		public GPSService getService()
		{
			return GPSService.this;
		}
	}

	/**
	 * Run when the service is started. Sets the application and datasource, starts GPS tracking,
	 * and returns START_STICKY as this Service is sticky.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		startGPS();
		showSearchingNotification();
		this.geoPoints = new ArrayList<GeoPoint>();
		return (START_STICKY);
	}

	/**
	 * Run when a walk should start being tracked (GPS is locked on, the user has pushed start).
	 * Sets trackingWalk to true and shows the tracking walk notification.
	 * 
	 */
	public void startTrackingWalk()
	{
		trackingWalk = true;
		showTrackingNotification();
	}

	/**
	 * Locks on to GPS and starts getting location updates. Gets the System LocationManager an
	 * uses a custom Listener to handle the incoming data.
	 */
	private void startGPS()
	{
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		this.locationListener = new CustomLocationListener();

		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.locationListener);
	}

	/**
	 * Shuts down everything related to tracking a walk.<br>
	 * <br>
	 * Specifically, removes the locationListener, which disables GPS, sets the manager to null, sets
	 * locked and isRunning to false, stops the notification and then stops itself.
	 */
	public void walkFinished()
	{
		this.locationManager.removeUpdates(this.locationListener);
		this.locationManager = null;
		
		this.gpsLocked = false;

		isRunning = false;

		stopForeground(true);

		stopSelf();
	}
	
	/////////////////////////
	//
	// Notification methods
	//
	/////////////////////////
	/**
	 * Shows the "Searching for GPS" notification. This also sets the service as a foreground
	 * service, so it's very unlikely to be killed.
	 */
	public void showSearchingNotification()
	{
		// Get a Resources as it's used several times
		Resources resources = getResources();

		Intent notificationIntent = new Intent(this, WalkListActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Build a notification.
		this.waitingForGPSNotification = new Notification.Builder(getApplicationContext())
				.setContentTitle(resources.getString(R.string.app_name))
				.setContentText(resources.getString(R.string.notification_waiting_for_gps))
				.setSmallIcon(R.drawable.notification)
				.setTicker(resources.getString(R.string.connecting_to_gps)).setContentIntent(contentIntent)
				.getNotification();
		// Set the notification as a foreground service, so it can't be dismissed
		this.waitingForGPSNotification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		// Start the service in the foreground
		startForeground(NOTIFICATION_INT, this.waitingForGPSNotification);
	}

	/**
	 * Shows the "Tracking walk" notification and sets up the "paused" notification.
	 */
	public void showTrackingNotification()
	{
		// Get a Resources as it's used several times
		Resources resources = getResources();

		Walk walk = DataSource.getWalkById(0);
		
		// Set up the intent and pending intent
		Intent notificationIntent = new Intent(this, MapWalkActivity.class);
		notificationIntent.putExtra(WalkListActivity.WALK_TAG, walk);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Build a notification.
		this.trackingNotification = new Notification.Builder(getApplicationContext())
				.setContentTitle(resources.getString(R.string.app_name))
				.setContentText(resources.getString(R.string.notification_walk) + " " + walk.getName())
				.setSmallIcon(R.drawable.notification).setTicker(resources.getString(R.string.notification_walk_ticker))
				.setContentIntent(contentIntent).getNotification();
		// Set the notification as a foreground service, so it can't be dismissed
		this.trackingNotification.flags |= Notification.FLAG_FOREGROUND_SERVICE;

		this.pausedNotification = new Notification.Builder(getApplicationContext())
				.setContentTitle(resources.getString(R.string.app_name))
				.setContentText(
						resources.getString(R.string.notification_walk) + " " + walk.getName() + " "
								+ resources.getString(R.string.paused)).setSmallIcon(R.drawable.notification_paused)
				.setTicker(resources.getString(R.string.notification_paused_ticker)).setContentIntent(contentIntent)
				.getNotification();

		// Start the service in the foreground
		startForeground(NOTIFICATION_INT, this.trackingNotification);

		// Make sure that gpsLocked is true so that the timer thread loop runs.
		if (!this.gpsLocked) this.gpsLocked = true;
		
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		// Start a new Thread - used to make the position only be updated every "updateTime" seconds
		this.timer = new Thread() {
			@Override
			public void run()
			{
				while (GPSService.this.gpsLocked)
				{
					GPSService.this.readyForUpdate = true;
					int updateRate = 1000 * (Integer.parseInt(preferences.getString(MAP_ACCURACY, "20")));
					try
					{
						Thread.sleep(updateRate);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		};
		this.timer.start();
	}

	/**
	 * Shows the "GPS Disabled" notification. This is used if the user disables GPS before
	 * tracking starts.
	 */
	public void showGPSDisabledNotification()
	{
		// Get a Resources as it's used several times
		Resources resources = getResources();

		Intent notificationIntent = new Intent(this, WalkListActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// Build a notification.
		Notification gpsDisabledNotification = new Notification.Builder(getApplicationContext())
				.setContentTitle(resources.getString(R.string.app_name))
				.setContentText(resources.getString(R.string.notification_gps_disabled))
				.setSmallIcon(R.drawable.notification_cancelled)
				.setTicker(resources.getString(R.string.notification_gps_disabled)).setContentIntent(contentIntent)
				.getNotification();

		gpsDisabledNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(DISMISSABLE_NOTIFICATION_INT, gpsDisabledNotification);
	}

	/////////////////////////
	//
	// Set methods
	//
	/////////////////////////
	/**
	 * Used by MapWalkActivity to inform the GPSService is the map is visible or not (set to
	 * false in onPause, true when the service connects and in onResume).
	 * 
	 * @param mapOpen	Whether the MapWalkActivity is visible or not.
	 */
	public void setMapOpen(boolean mapOpen)
	{
		this.mapOpen = mapOpen;
	}

	/**
	 * Used by WalkListActivity to let the service call methods in the activity.
	 * 
	 * @param walkListActivity	The activity instance
	 */
	public void setWalkListActivity(WalkListActivity walkListActivity)
	{
		this.walkListActivity = walkListActivity;
	}

	/**
	 * Used by MapWalkActivity to let the service call methods in the activity.
	 * 
	 * @param mapWalkActivity	The activity instance
	 */
	public void setMapWalkActivity(MapWalkActivity mapWalkActivity)
	{
		this.mapWalkActivity = mapWalkActivity;
	}

	/////////////////////////
	//
	// Get methods
	//
	/////////////////////////
	/**
	 * Returns the geoPoints stored for the walk. Called by MapWalkActivity.
	 * 
	 * @return	The stored geoPoints for the in-progress walk.
	 */
	public ArrayList<GeoPoint> getGeoPoints()
	{
		return this.geoPoints;
	}

	/**
	 * Returns the paused status of the service. Used by MapWalkActivity to show
	 * the appropriate button on the menu.
	 * 
	 * @return	Whether the service is paused or not.
	 */
	public boolean getPaused()
	{
		return this.paused;
	}

	/////////////////////////
	//
	// Pause/Resume methods
	//
	/////////////////////////
	/**
	 * Run when the user pauses an in-progress walk. Shows the paused notification, sets paused
	 * to true, unregisters the locationListener to shut off GPS (saving power), and sets GPSLocked
	 * to false.
	 */
	public void pause()
	{
		startForeground(NOTIFICATION_INT, this.pausedNotification);
		this.paused = true;
		this.locationManager.removeUpdates(this.locationListener);
		this.gpsLocked = false;
	}

	/**
	 * Run when the user resumes a paused walk. Shows the Waiting for GPS notification (as it needs
	 * to reconnect to GPS), sets paused to false, resumePressed to true, and re-initialises up the
	 * locationManager.
	 */
	public void resume()
	{
		startForeground(NOTIFICATION_INT, this.waitingForGPSNotification);
		this.paused = false;
		this.resumePressed = true;
		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this.locationListener);
	}

	/////////////////////////
	//
	// Inner Classes
	//
	/////////////////////////
	/**
	 * Custom LocationListener class.<br>
	 * <br>
	 * When a location is received, it stores the latitude and longitude, and then checks if "GPSLocked" is true.
	 * If it isn't, then this is the first location received, and it informs the application that GPS is locked,
	 * and stores the first geoPoint.<br>
	 * <br>
	 * Otherwise, if the resume key has been pressed, it shows the Tracking Walk notification.
	 * <br>
	 * Then, if it is tracking a walk (so the user has chosen "start walk"), and it's ready for an update
	 * (i.e. the set period of time has passed), it gets the latitude and longitude, inserts them into the database with
	 * a walk ID of 0 (0 being reserved for walks in progress), creates a new GeoPoint, adds it to the application's
	 * temporary ArrayList which is used only while a walk is in progress, and sets readyForUpdate to false.<br>
	 * <br>
	 * @author Alex Hardwicke
	 */
	public class CustomLocationListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location)
		{
			double lat = location.getLatitude();
			double lon = location.getLongitude();

			// If GPS isn't locked set it as locked in the app and service.
			if (!GPSService.this.gpsLocked)
			{
				if (GPSService.this.walkListActivity != null) GPSService.this.walkListActivity.setGPSLocked();

				// Creating the first GPS point. Will be retrieved by mapWalkActivity when it's opened.
				DataSource.createGeoPoint(0, lat, lon);
				GeoPoint geoPoint = new GeoPoint((int) (lat * 1e6), (int) (lon * 1e6));
				GPSService.this.geoPoints.add(geoPoint);

				GPSService.this.gpsLocked = true;
			}
			
			// first location update post-user resume. show the "gps locked" notification
			// (which also starts the update timer again) and then clears resumePressed so
			// that the next if statement is run on the next location grab
			else if (GPSService.this.resumePressed)
			{
				showTrackingNotification();
				GPSService.this.resumePressed = false;
			}

			// If tracking the walk (user has started, activity is ready for an update), record point
			if (trackingWalk && GPSService.this.readyForUpdate)
			{
				DataSource.createGeoPoint(0, lat, lon);
				GeoPoint geoPoint = new GeoPoint((int) (lat * 1e6), (int) (lon * 1e6));
				GPSService.this.geoPoints.add(geoPoint);
				if (GPSService.this.mapOpen)
				{
					if (GPSService.this.mapWalkActivity != null)
						GPSService.this.mapWalkActivity.newPoint(geoPoint);
				}
				GPSService.this.readyForUpdate = false;
			}
		}

		@Override
		public void onProviderDisabled(String provider)
		{
			// User has disabled GPS. If not tracking a walk, show a "GPS disabled. Ending..." notification.
			// If tracking a walk, pause.
			if (!trackingWalk)
			{
				if (GPSService.this.walkListActivity.isInForeground())
					Toast.makeText(GPSService.this.walkListActivity, getString(R.string.notification_gps_disabled), Toast.LENGTH_LONG).show();
				else
				{
					showGPSDisabledNotification();
					isCancelled = true;
				}
				GPSService.this.walkListActivity.cancelGPS();
			}
			else
				pause();
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			// User has re-enabled GPS. Resume tracking!
			resume();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}
	}
}