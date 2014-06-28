package com.digitalpies.promenade.database;

/**
 * Class used to manage Photo data once retrieved from the database
 * 
 * It's just a MapOverlayItem that stores additional data in the form
 * of a String containing the file:// details.
 * 
 * @author Alex Hardwicke
 */
public class Photo extends MapOverlayItem
{
	private String file;
	
	public Photo(long id, double latitude, double longitude, String file)
	{
		super(id, latitude, longitude);
		this.file = file;
	}

	public String getFile()
	{
		return this.file;
	}
	
	public void setFile(String file)
	{
		this.file = file;
	}
}
