package com.digitalpies.promenade.maps;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.digitalpies.promenade.R;
import com.google.android.maps.MapView;

/**
 * Custom implementation of MapView.<br>
 * <br>
 * When the map is drawn, it compares the zoom level. If it's 1, it disables the zoom in button.
 * If it's max, it disables the zoom out button. If it's 2 and was previously 1, it enables the
 * zoom in button, and if it's max-1 and was max previously, it enables the zoom out button.<br>
 * <br>
 * When scrolling has to be recalculated (so a zoom has been performed), it removes the custom
 * Runnable if it's on, and posts it again, with a delay (TODO delay needs to be configured,
 * value of 100 works fine on SGS2 but might not on slower phones). When the runnable is run,
 * it updates the map (which merges icons) and updates the previousZoom value.
 * 
 * @author Alex Hardwicke
 */
public class CustomMapView extends MapView
{
	// REFERENCE_NOTE test on slow phones, see if delay is too low
	private static final long delay = 100;
	private CustomMapActivity activity;
	private ImageView zoomIn;
	private ImageView zoomOut;
	private int previousZoom;
	private ListenRunnable runnable = new ListenRunnable();

	public CustomMapView(Context context, String apiKey)
	{
		super(context, apiKey);
	}

	public CustomMapView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CustomMapView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	/**
	 * Provides the MapView with references to the CustomMapActivity, and the zoomIn and zoomOut buttons.
	 * 
	 * @param activity	The activity the MapView is in.
	 * @param zoomIn	The zoom in button.
	 * @param zoomOut	The zoom out button.
	 */
	public void setUp(CustomMapActivity activity, ImageView zoomIn, ImageView zoomOut)
	{
		this.activity = activity;
		this.zoomIn = zoomIn;
		this.zoomOut = zoomOut;
	}

	/**
	 * Overrides dispatchDraw to perform changes whenever the view is zoomed.<br>
	 * <br>
	 * Checks the zoom level. If it's fully zoomed in, disables the zoom in button. If it's one away from
	 * fully zoomed in, enables the zoom in button. Does the same but for fully zoomed out for the zoom
	 * out button.<br>
	 * <br>
	 * Then compares the zoom levels - if they don't match, it starts the runnable with the delay value.
	 */
	@Override
	public void dispatchDraw(Canvas canvas)
	{
		super.dispatchDraw(canvas);
		if (getZoomLevel() == 1)
			this.zoomOut.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_out_disabled));
		else if (getZoomLevel() == 2)
			this.zoomOut.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_out));
		else if (getZoomLevel() == (getMaxZoomLevel() - 1))
			this.zoomIn.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_in));
		else if (getZoomLevel() == getMaxZoomLevel())
			this.zoomIn.setImageDrawable(getResources().getDrawable(R.drawable.ic_map_zoom_in_disabled));

		if (getZoomLevel() != this.previousZoom)
		{
			this.removeCallbacks(this.runnable);
			this.postDelayed(this.runnable, delay);
		}
	}

	/**
	 * Custom implementation of Runnable that redraws the map and updates the zoom level.
	 */
	private class ListenRunnable implements Runnable
	{
		@Override
		public void run()
		{
			CustomMapView.this.activity.drawOverlays();
			CustomMapView.this.previousZoom = getZoomLevel();
		}
	}
}
