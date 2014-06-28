package com.digitalpies.promenade.database;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class used to manage Tag data once retrieved from the database.
 * 
 * @author Alex Hardwicke
 */
public class Tag implements Comparable<Tag>, Parcelable
{
	private String name;

	public Tag(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Converts the supplied String array of tags into an ArrayList<Tag>.<br>
	 * <br>
	 * Iterates through the entire String array, adding each String to the ArrayList.
	 * 
	 * @param tagArray String[]	The String-array of tags.
	 * 
	 * @return null if tagArray is empty, otherwise an ArrayList with the tags in.
	 */
	public static ArrayList<Tag> stringArrayToList(String[] tagArray)
	{
		if (tagArray.length == 0)
			return null;
		ArrayList<Tag> tagList = new ArrayList<Tag>();
		for (String tagString : tagArray)
		{
			tagList.add(new Tag(tagString));
		}
		return tagList;
	}

	@Override
	public int compareTo(Tag tag)
	{
		return this.name.compareTo(tag.getName());
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object instanceof Tag)
			return this.name.equals(((Tag) object).getName());
		return object.equals(this);
	}
	
	@Override
	public int hashCode()
	{
		return this.name.hashCode();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(this.name);
	}
	
	public static Creator<Tag> CREATOR = new Creator<Tag>()
	{
		@Override
		public Tag createFromParcel(Parcel arg0)
		{
			return new Tag(arg0.readString());
		}

		@Override
		public Tag[] newArray(int size)
		{
			return new Tag[size];
		}
	};
}