package com.digitalpies.promenade.database;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Class used to manage Walk data once retrieved from the database. Implements Parcelable
 * as it needs to be Bundled between Activities.
 * 
 * @author Alex Hardwicke
 */
public class Walk implements Parcelable
{
	private long id, date;
	private String name, description;
	private ArrayList<Tag> tags = new ArrayList<Tag>();
	
	public static String TAGS = "TAGS";
	
	public Walk(long id, String name, String description, Long date, ArrayList<Tag> tags)
	{
		this.id = id;
		this.name = name;
		this.description = description;
		this.date = date;
		for (Tag tag : tags)
			if (tag.getName() != "") this.tags.add(tag);
	}
	
	public Walk(Parcel parcel)
	{
		this.id = parcel.readLong();
		this.name = parcel.readString();
		this.description = parcel.readString();
		this.date = parcel.readLong();
		parcel.readTypedList(this.tags, Tag.CREATOR);
	}
	
	public long getId()
	{
		return this.id;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return this.description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}

	public ArrayList<Tag> getTags()
	{
		return this.tags;
	}
		
	public void setTags(ArrayList<Tag> tags)
	{
		this.tags = tags;
	}

	public Long getDate()
	{
		return this.date;
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int arg1)
	{
		dest.writeLong(this.id);
		dest.writeString(this.name);
		dest.writeString(this.description);
		dest.writeLong(this.date);
		dest.writeTypedList(this.tags);
	}
	
	public static Creator<Walk> CREATOR = new Creator<Walk>()
	{
		@Override
		public Walk createFromParcel(Parcel parcel)
		{
			return new Walk(parcel);
		}

		@Override
		public Walk[] newArray(int size)
		{
			return new Walk[size];
		}
	};
}
