package com.digitalpies.promenade.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class manages creating and upgrading the database, and contains all of the Strings needed to access any table
 * and column in the database.
 * 
 * @author Alex Hardwicke
 */
public class SQLiteHelper extends SQLiteOpenHelper
{
	public static final String DATABASE_NAME = "promenade.db";
	public static final int DATABASE_VERSION = 8;

	public static final String COLUMN_ID = "_id";

	public static final String TABLE_WALKS = "walks";
	public static final String TABLE_GPS = "gps";
	public static final String TABLE_PHOTOS = "photos";
	public static final String TABLE_NOTES = "notes";
	public static final String TABLE_SEARCH = "search";

	public static final String WALKS_NAME = "name";
	public static final String WALKS_DESCRIPTION = "description";
	public static final String WALKS_TAGS = "tags";
	public static final String WALKS_DATE = "date";

	public static final String GPS_WALK_ID = "walk_id";
	public static final String GPS_LATITUDE = "latitude";
	public static final String GPS_LONGITUDE = "longitude";

	public static final String PHOTOS_WALK_ID = "walk_id";
	public static final String PHOTOS_LATITUDE = "latitude";
	public static final String PHOTOS_LONGITUDE = "longitude";
	public static final String PHOTOS_FILE = "file";

	public static final String NOTES_WALK_ID = "walk_id";
	public static final String NOTES_LATITUDE = "latitude";
	public static final String NOTES_LONGITUDE = "longitude";
	public static final String NOTES_NOTE = "note";

	public static final String SEARCH_WALK_ID = "walk_id";
	public static final String SEARCH_WALK_NAME = "name";
	public static final String SEARCH_WALK_DESCRIPTION = "description";
	public static final String SEARCH_WALK_TAGS = "tags";

	public static final String WALKS_CREATE = "create table " + TABLE_WALKS + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + WALKS_NAME + " text not null, " + WALKS_DESCRIPTION
			+ " text not null, " + WALKS_TAGS + " text not null, " + WALKS_DATE + " text not null);";
	public static final String GPS_CREATE = "create table " + TABLE_GPS + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + GPS_WALK_ID + " integer, " + GPS_LATITUDE + " real, "
			+ GPS_LONGITUDE + " real);";
	public static final String PHOTOS_CREATE = "create table " + TABLE_PHOTOS + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + PHOTOS_WALK_ID + " integer, " + PHOTOS_LATITUDE + " real, "
			+ PHOTOS_LONGITUDE + " real, " + PHOTOS_FILE + " text not null);";
	public static final String NOTES_CREATE = "create table " + TABLE_NOTES + "(" + COLUMN_ID
			+ " integer primary key autoincrement, " + NOTES_WALK_ID + " integer, " + NOTES_LATITUDE + " real, "
			+ NOTES_LONGITUDE + " real, " + NOTES_NOTE + " text not null);";
	public static final String SEARCH_CREATE = "create virtual table " + TABLE_SEARCH + " using FTS3 (" + SEARCH_WALK_ID
			+ " integer, " + SEARCH_WALK_NAME + " TEXT, " + SEARCH_WALK_DESCRIPTION + " TEXT, " + SEARCH_WALK_TAGS + ", TEXT);";

	public SQLiteHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database)
	{
		database.execSQL(WALKS_CREATE);
		database.execSQL(GPS_CREATE);
		database.execSQL(PHOTOS_CREATE);
		database.execSQL(NOTES_CREATE);
		database.execSQL(SEARCH_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
	{
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_WALKS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_GPS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTOS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH);
		onCreate(database);
	}
}
