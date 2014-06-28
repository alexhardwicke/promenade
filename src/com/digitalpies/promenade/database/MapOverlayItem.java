package com.digitalpies.promenade.database;

import com.google.android.maps.GeoPoint;

/**
 * An abstract class extended by the Note and Photo classes. Both classes have
 * an id and latitude and longitude values. These are stored as a GeoPoint (as
 * that is how they are used by every class that accesses these OverlayItems).
 *  
 * @author Alex Hardwicke
 */
public abstract class MapOverlayItem
{
	protected long id;
	protected GeoPoint geoPoint;

	public MapOverlayItem(long id, double latitude, double longitude)
	{
		this.id = id;
		this.geoPoint = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
	}
	
	public long getId()
	{
		return this.id;
	}
		
	public GeoPoint getGeoPoint()
	{
		return this.geoPoint;
	}
}
