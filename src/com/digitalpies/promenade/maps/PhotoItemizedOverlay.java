package com.digitalpies.promenade.maps;

import com.digitalpies.promenade.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

/**
 * Photo version of CustomItemizedOverlay.<br>
 * <br>
 * Overrides createBalloonOverlayView to return a custom photo view.<br>
 * <br>
 * Overrides onBalloonOpen. Runs the super version, and then calculates a point at the centre of the
 * screen horizontally, close to the bottom vertically, and zooms the camera to that point.
 * 
 * @author Alex Hardwicke
 */
public class PhotoItemizedOverlay extends CustomItemizedOverlay
{
	public static final String FIRST_PHOTO_RUN = "FIRST_PHOTO_RUN";
	private String type;

	public PhotoItemizedOverlay(Drawable defaultMarker, CustomMapActivity activity, MapView mapView, String type)
	{
		super(defaultMarker, activity, mapView);

		this.type = type;

		// SnapToCenter false, as alternative zooming is set up in onBalloonOpen
		setSnapToCenter(false);
	}

	@Override
	protected CustomOverlayView<OverlayItem> createBalloonOverlayView()
	{
		return new CustomPhotoView(this.mapView.getContext(), getBalloonBottomOffset(), this.activity);
	}

	/**
	 * Returns a custom view with no title.
	 */
	@Override
	protected void onBalloonOpen(int index)
	{
		super.onBalloonOpen(index);

		DisplayMetrics metrics = this.activity.getResources().getDisplayMetrics();

		// Get values used often
		int mapHeight = this.mapView.getHeight();
		int mapZoom = this.mapView.getZoomLevel();
		Projection proj = this.mapView.getProjection();

		// Find the GeoPoint for the bottom of the map
		GeoPoint bottomGeoPoint = proj.fromPixels(0, mapHeight);

		// Find the GeoPoint for a point somewhat above the bottom of the map. Takes into account screen density
		// and, at the 6 outermost zoom levels, varied values
		GeoPoint aboveGeoPoint;

		if (mapZoom > 6)
			aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (240 * metrics.density)));
		else
		{
			// Need to use different calculation for the 6 outermost zoom levels
			switch (mapZoom)
			{
			case 6:
				aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (225 * metrics.density)));
				break;
			case 5:
				aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (210 * metrics.density)));
				break;
			case 4:
				aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (195 * metrics.density)));
				break;
			case 3:
				aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (180 * metrics.density)));
				break;
			case 2:
				aboveGeoPoint = proj.fromPixels(0, (int) (metrics.heightPixels - (160 * metrics.density)));
				break;
			default:
				aboveGeoPoint = proj.fromPixels(0, metrics.heightPixels);
				break;
			}
		}

		// Calculate the difference in latitude between the bottom of the map and the calculated point
		int difference = aboveGeoPoint.getLatitudeE6() - bottomGeoPoint.getLatitudeE6();

		// Retrieves the GeoPoint of the tapped item - from the Stacked or normal overlay, depending on
		// whether the overlay is stacked or not
		GeoPoint point = null;
		if (this.type == CustomMapActivity.STACKED_PHOTO)
			point = this.activity.photoStackedOverlay.getItem(index).getPoint();
		else if (this.type == CustomMapActivity.PHOTO)
			point = this.activity.photoOverlay.getItem(index).getPoint();

		// If the point isn't null (safety check), create a GeoPoint "finalPoint", modifies the latitude by the
		// difference value, and moves the camera to the point
		if (point != null)
		{
			GeoPoint finalPoint = new GeoPoint(point.getLatitudeE6() + difference, point.getLongitudeE6());
			animateTo(index, finalPoint);
		}

		// Get the SharedPreferences and check if this is the first time the user is at this balloon. If so, inform
		// them with a Toast that they swipe to view other photos, and set the preferences so that this can't
		// happen again. Only runs if there is more than one photo.
		if (this.activity.getPhotos().size() > 0)
		{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.activity);
			if (preferences.getBoolean(FIRST_PHOTO_RUN, true))
			{
				Toast.makeText(this.activity, R.string.toast_can_swipe_photos, Toast.LENGTH_LONG).show();
				Editor editor = preferences.edit();
				editor.putBoolean(FIRST_PHOTO_RUN, false);
				editor.apply();
			}
		}
	}
}
