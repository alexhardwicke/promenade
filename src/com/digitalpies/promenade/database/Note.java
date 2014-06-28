package com.digitalpies.promenade.database;

/**
 * Class used to manage Note data once retrieved from the database.
 * 
 * It's just a MapOverlayItem that stores additional data in the form
 * of a String containing the note.
 * 
 * @author Alex Hardwicke
 */
public class Note extends MapOverlayItem
{
	private String note;
	
	public Note(long id, double latitude, double longitude, String note)
	{
		super(id, latitude, longitude);
		this.note = note;
	}

	public String getNote()
	{
		return this.note;
	}
	
	public void setNote(String note)
	{
		this.note = note;
	}
}
