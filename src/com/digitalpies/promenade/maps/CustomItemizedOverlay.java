package com.digitalpies.promenade.maps;

import java.util.ArrayList;

import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;

import android.graphics.drawable.Drawable;

/**
 * Custom version of BalloonItemizedOverlay. Cannot be instantiated directly, but is extended
 * by NoteItemizedOverlay and PhotoItemizedOverlay.<br>
 * <br>
 * Has a reference to the creating activity (used by the extending classes to show dialogues),
 * an ArrayList of overlayitems, and a reference the mapview, used to clear the mapview when the
 * overlay is cleared.
 * 
 * @author Alex Hardwicke
 */
public abstract class CustomItemizedOverlay extends BalloonItemizedOverlay<OverlayItem>
{
	protected CustomMapActivity activity;
	protected ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
	protected MapView mapView;

	public CustomItemizedOverlay(Drawable defaultMarker, CustomMapActivity activity, MapView mapView)
	{
		super(boundCenterBottom(defaultMarker), mapView);
		setBalloonBottomOffset(defaultMarker.getIntrinsicHeight());
		this.activity = activity;
		this.mapView = mapView;
		populate();
	}
	
	/**
	 * When a balloon is tapped, informs the activity (which opens an action mode), and provides
	 * the position of the item, and whether or not it's a photo.
	 */
	@Override
	protected void onBalloonOpen(int index)
	{
		boolean isPhoto = (this instanceof PhotoItemizedOverlay);
		this.activity.itemTapped(Integer.parseInt(this.overlays.get(index).getTitle()), isPhoto);
	}

	public void addOverlay(OverlayItem overlay)
	{
		this.overlays.add(overlay);
		setLastFocusedIndex(-1);
		populate();
	}

	public void removeOverlay(OverlayItem overlay)
	{
		this.overlays.remove(overlay);
		populate();
	}

	public void clear()
	{
		this.overlays.clear();
		this.mapView.removeAllViews();
		setLastFocusedIndex(-1);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i)
	{
		return this.overlays.get(i);
	}

	@Override
	public int size()
	{
		return this.overlays.size();
	}
}
