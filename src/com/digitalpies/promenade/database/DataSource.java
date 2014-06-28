package com.digitalpies.promenade.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import com.digitalpies.promenade.walklist.CustomListActivity;
import com.google.android.maps.GeoPoint;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Manages all interaction with the database. Contains a large number of methods for adding, removing, retrieving
 * and editing rows in the database.
 * 
 * The entire class barring the constructor consists of static methods and variables - allows access to the same
 * databaseHelper instance regardless of source (and multiple connections to a database end badly in Android).
 * 
 * @author Alex Hardwicke
 */
public class DataSource
{
	private static SQLiteDatabase database;
	private static SQLiteHelper databaseHelper;
	private static DataSource datasource = null;
	public final static String TAG_PADDING = " , ";

	// Arrays for each table in the database - each one contains all the columns for tha table
	private static final String[] walksColumns = { SQLiteHelper.COLUMN_ID, SQLiteHelper.WALKS_NAME,
			SQLiteHelper.WALKS_DESCRIPTION, SQLiteHelper.WALKS_TAGS, SQLiteHelper.WALKS_DATE };
	private static final String[] gpsColumns = { SQLiteHelper.COLUMN_ID, SQLiteHelper.GPS_WALK_ID,
			SQLiteHelper.GPS_LATITUDE, SQLiteHelper.GPS_LONGITUDE };
	private static final String[] photosColumns = { SQLiteHelper.COLUMN_ID, SQLiteHelper.PHOTOS_WALK_ID,
			SQLiteHelper.PHOTOS_LATITUDE, SQLiteHelper.PHOTOS_LONGITUDE, SQLiteHelper.PHOTOS_FILE };
	private static final String[] notesColumns = { SQLiteHelper.COLUMN_ID, SQLiteHelper.NOTES_WALK_ID,
			SQLiteHelper.NOTES_LATITUDE, SQLiteHelper.NOTES_LONGITUDE, SQLiteHelper.NOTES_NOTE };
	private static final String[] searchColumns = { SQLiteHelper.SEARCH_WALK_ID, SQLiteHelper.SEARCH_WALK_NAME,
			SQLiteHelper.SEARCH_WALK_DESCRIPTION, SQLiteHelper.SEARCH_WALK_TAGS };

	// ///////////////////////////////////////////
	//
	// DataSource methods
	//
	// ///////////////////////////////////////////
	private DataSource(Context context)
	{
		DataSource.databaseHelper = new SQLiteHelper(context);
	}

	// Allows static creation of the datasource. Creates a DataSource object and opens it.
	public static void openDataSource(Context context)
	{
		if (datasource == null)
		{
			datasource = new DataSource(context);
			DataSource.database = DataSource.databaseHelper.getWritableDatabase();
		}
	}

	// ///////////////////////////////////////////
	//
	// Get Methods
	//
	// ///////////////////////////////////////////
	/**
	 * Retrieves all of the Tags from each Walk, skipping duplicates, sorts them into
	 * alphabetical order and returns them as an ArrayList<Tag>.
	 * 
	 * @return	A List containing the Tags
	 */
	public static ArrayList<Tag> getAllTags()
	{
		// A HashSet, used to filter out duplicates
		HashSet<Tag> tagHashSet = new HashSet<Tag>();
		ArrayList<Walk> walks = getAllWalks(0);

		// Go through each walk, get the tags and add them to the HashSet. This means the HashSet
		// contains every tag and no duplicates
		for (Walk walk : walks)
		{
			if (walk.getTags() != null) tagHashSet.addAll(walk.getTags());
		}

		// Convert to ArrayList, sort, return.
		ArrayList<Tag> tags = new ArrayList<Tag>(tagHashSet);
		Collections.sort(tags);
		return tags;
	}

	/**
	 * Retrieves all of the Walks from the database and returns them.<br>
	 * <br>
	 * Switches through the sortValue integer provided to sort the database result appropriately.<br>
	 * <br>
	 * @param sortValue	An integer representing the required sort order.
	 * 
	 * @return	A List containing the Walks
	 */
	public static ArrayList<Walk> getAllWalks(int sortValue)
	{
		ArrayList<Walk> walks = new ArrayList<Walk>();

		String sort = "";
		switch (sortValue)
		{
		case CustomListActivity.DATE_ASCENDING:
			sort = SQLiteHelper.COLUMN_ID + " ASC";
			break;
		case CustomListActivity.DATE_DESCENDING:
			sort = SQLiteHelper.COLUMN_ID + " DESC";
			break;
		case CustomListActivity.NAME_ASCENDING:
			sort = SQLiteHelper.WALKS_NAME + " ASC";
			break;
		case CustomListActivity.NAME_DESCENDING:
			sort = SQLiteHelper.WALKS_NAME + " DESC";
			break;
		}
		// Retrieve all Walks
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_WALKS, DataSource.walksColumns, null, null,
				null, null, sort);
		cursor.moveToFirst();

		// Add each Walk to the List
		while (!cursor.isAfterLast())
		{
			Walk walk = cursorToWalk(cursor);
			walks.add(walk);
			cursor.moveToNext();
		}

		cursor.close();
		return walks;
	}

	/**
	 * Retrieves a walk from the database with an id matching the provided ID.
	 * 
	 * @param id	The ID of the walk.
	 * 
	 * @return	The requested walk.
	 */
	public static Walk getWalkById(long id)
	{
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_WALKS, DataSource.walksColumns,
				SQLiteHelper.COLUMN_ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		Walk walk = cursorToWalk(cursor);
		cursor.close();
		return walk;
	}

	/**
	 * Retrieves all of the Note objects from the database for the provided Walk and returns them.
	 * 
	 * @return	A List containing the Note objects for the provided Walk
	 */
	public static ArrayList<Note> getNotesForWalk(Walk walk)
	{
		ArrayList<Note> notes = new ArrayList<Note>();

		// Get all Notes for the provided Walk
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_NOTES, DataSource.notesColumns,
				SQLiteHelper.NOTES_WALK_ID + " = " + walk.getId(), null, null, null, null);
		cursor.moveToFirst();

		// Add each Note to the List
		while (!cursor.isAfterLast())
		{
			Note note = cursorToNote(cursor);
			notes.add(note);
			cursor.moveToNext();
		}

		cursor.close();
		return notes;
	}

	/**
	 * Retrieves all of the GeoPoints from the database for the provided Walk and returns them.
	 * 
	 * @return	A List containing the GPS points for the provided Walk
	 */
	public static ArrayList<GeoPoint> getGeoPointsForWalk(Walk walk)
	{
		ArrayList<GeoPoint> geoPoints = new ArrayList<GeoPoint>();

		// Retrieve all GPS points for the provided Walk
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_GPS, DataSource.gpsColumns,
				SQLiteHelper.PHOTOS_WALK_ID + " = " + walk.getId(), null, null, null, null);
		cursor.moveToFirst();

		// Add each GPS point to the List
		while (!cursor.isAfterLast())
		{
			GeoPoint geoPoint = cursorToGeoPoint(cursor);
			geoPoints.add(geoPoint);
			cursor.moveToNext();
		}

		cursor.close();
		return geoPoints;
	}

	/**
	 * Retrieves all of the Photo objects from the database for the provided Walk and returns them.
	 * 
	 * @return	A List containing the Photo objects for the provided Walk
	 */
	public static ArrayList<Photo> getPhotosForWalk(Walk walk)
	{
		ArrayList<Photo> photos = new ArrayList<Photo>();

		// Get all Photos for the provided Walk
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_PHOTOS, DataSource.photosColumns,
				SQLiteHelper.PHOTOS_WALK_ID + " = " + walk.getId(), null, null, null, null);
		cursor.moveToFirst();

		// Add each Photo to the List
		while (!cursor.isAfterLast())
		{
			Photo photo = cursorToPhoto(cursor);
			photos.add(photo);
			cursor.moveToNext();
		}

		cursor.close();
		return photos;
	}

	/**
	 * Retrieves all of the Note objects from the database for the provided Walk and returns them.
	 * 
	 * @return	A List containing the Note objects for the provided WAlk
	 */
	public static int getNoteCountForWalk(Walk walk)
	{
		// Get all Notes for the provided Walk
		return DataSource.database.query(SQLiteHelper.TABLE_NOTES, DataSource.notesColumns,
				SQLiteHelper.NOTES_WALK_ID + " = " + walk.getId(), null, null, null, null).getCount();
	}

	/**
	 * Returns the number of rows from the photo table that match the current walk.
	 * 
	 * @return	The number of Photo objects in the database for the provided Walk
	 */
	public static int getPhotoCountForWalk(Walk walk)
	{
		// Get all Photos for the provided Walk
		return DataSource.database.query(SQLiteHelper.TABLE_PHOTOS, DataSource.photosColumns,
				SQLiteHelper.PHOTOS_WALK_ID + " = " + walk.getId(), null, null, null, null).getCount();
	}

	// ///////////////////////////////////////////
	//
	// Create methods
	//
	// ///////////////////////////////////////////
	/**
	 * Inserts a temporary Walk into the database with the id of 0. This ID is reserved for a walk in progress<br>
	 * <br>
	 * It first clears the GeoPoints, Photos and Notes stored in ID 0 (in case the user was taking a walk and their
	 * phone crashed, or they killed the app). Then it deletes the actual walk from the database.<br>
	 * <br>
	 * It then sorts the ArrayList and then iterates through it, adding each tag to a String, divided by the
	 * TAG_PADDING variable.<br>
	 * <br>
	 * Finally, it gets the current time value in milliseconds and inserts that into the database.<br>
	 * <br>
	 * @param name 			The walk's name.
	 * @param description 	The walk's description.
	 * @param tags 			The walk's tags in String form.
	 * 
	 * @return						Returns the created Walk
	 */
	public static Walk createTemporaryWalk(String name, String description, ArrayList<Tag> tags)
	{
		// Deleting the GPS data and photos for the temporary.
		// Not done async because these must be purged before anything else happens.
		deleteGeoPointForWalk(0);
		deletePhotosForWalk(0);
		deleteNotesForWalk(0);

		// Removing the entry from the database
		DataSource.database.delete(SQLiteHelper.TABLE_WALKS, SQLiteHelper.COLUMN_ID + " = 0", null);

		Collections.sort(tags);
		String tagString = "";
		for (Tag tag : tags)
		{
			tagString += tag.getName() + TAG_PADDING;
		}

		Date date = new Date();
		Long dateLong = date.getTime();

		// Creating the ContentValues and inserting the appropriate value
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COLUMN_ID, 0);
		values.put(SQLiteHelper.WALKS_NAME, name);
		values.put(SQLiteHelper.WALKS_DESCRIPTION, description);
		values.put(SQLiteHelper.WALKS_TAGS, tagString);
		values.put(SQLiteHelper.WALKS_DATE, dateLong);

		// Inserting the object into the database, retrieving it as a Cursor, creating the required object and
		// returning it.
		long insertId = DataSource.database.insert(SQLiteHelper.TABLE_WALKS, null, values);
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_WALKS, DataSource.walksColumns,
				SQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Walk walk = cursorToWalk(cursor);
		cursor.close();

		return walk;
	}

	/**
	 * Creates a GeoPoint in the database.<br>
	 * <br>
	 * Creates a ContentValues object, inserting the values and inserts it into the database.
	 * 
	 * @param walk_id	The id of the walk for which the GPS point belongs
	 * @param latitude	The latitude of the GPS point
	 * @param longitude	The longitude of the GPS point
	 * 
	 * @return			Returns the newly created GPS object
	 */
	public static void createGeoPoint(long walk_id, double latitude, double longitude)
	{
		// Creating the ContentValues and inserting the appropriate values
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.GPS_WALK_ID, walk_id);
		values.put(SQLiteHelper.GPS_LATITUDE, latitude);
		values.put(SQLiteHelper.GPS_LONGITUDE, longitude);

		// Inserting the object into the database
		if (DataSource.database != null)
			DataSource.database.insert(SQLiteHelper.TABLE_GPS, null, values);
	}

	/**
	 * Inserts a Photo into the database.<br>
	 * <br>
	 * Creates a Photo by creating a ContentValues object, inserting the values and inserts it into the database.
	 * 
	 * @param walk_id	The id of the walk to which the Photo belongs
	 * @param latitude	The latitude of the Photo
	 * @param longitude	The longitude of the Photo
	 * @param file		The photo's file location
	 */
	public static void createPhoto(long walk_id, double latitude, double longitude, String file)
	{
		// Creating the ContentValues and inserting the appropriate values
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.PHOTOS_WALK_ID, walk_id);
		values.put(SQLiteHelper.PHOTOS_LATITUDE, latitude);
		values.put(SQLiteHelper.PHOTOS_LONGITUDE, longitude);
		values.put(SQLiteHelper.PHOTOS_FILE, file);

		// Inserting the object into the database
		if (DataSource.database != null)
			DataSource.database.insert(SQLiteHelper.TABLE_PHOTOS, null, values);
	}

	/**
	 * Creates a Note by creating a ContentValues object, inserting the values, inserts it into the database,
	 * creates a Cursor by retrieving the just-entered row from the database, sends the Cursor to the cursorToNote
	 * function, and returns the created Note.
	 * 
	 * @param walk_id	The id of the walk to which the Note belongs
	 * @param latitude	The latitude of the Note
	 * @param longitude	The longitude of the Note
	 * @param file		The note text
	 * 
	 * @return			Returns the newly created Note object
	 */
	public static Note createNote(long walk_id, double latitude, double longitude, String noteText)
	{
		// Creating the ContentValues and inserting the appropriate values
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.NOTES_WALK_ID, walk_id);
		values.put(SQLiteHelper.NOTES_LATITUDE, latitude);
		values.put(SQLiteHelper.NOTES_LONGITUDE, longitude);
		values.put(SQLiteHelper.NOTES_NOTE, noteText);

		// Inserting the object into the database, retrieving it as a Cursor, creating the required object and
		// returning it.
		long insertId = DataSource.database.insert(SQLiteHelper.TABLE_NOTES, null, values);
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_NOTES, DataSource.notesColumns,
				SQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Note note = cursorToNote(cursor);
		cursor.close();
		return note;
	}

	// ///////////////////////////////////////////
	//
	// Edit methods
	//
	// ///////////////////////////////////////////
	/**
	 * Updates a Walk in the database.<br>
	 * <br>
	 * Retrieves the details from the Walk object, iterates through the tag ArrayList, adding
	 * each tag to a String, divided by TAG_PADDING, creates a ContentValues using these
	 * values, and performs an SQL update with the provided data.
	 * 
	 * @param walk	The new walk that needs to be entered into the database.
	 */
	public static void editWalk(Walk walk)
	{
		// Getting the values from the walk object
		ArrayList<Tag> tags = walk.getTags();
		Collections.sort(tags);
		String name = walk.getName();
		String description = walk.getDescription();
		long id = walk.getId();

		// Putting all of the tags into String form, padded with the TAG_PADDING string
		String tagString = "";
		if (tags != null)
		{
			// Converting back to a single String
			for (Tag tag : tags)
			{
				tagString += tag.getName() + TAG_PADDING;
			}
		}

		// Creating the ContentValues and inserting the appropriate values
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.WALKS_NAME, name);
		values.put(SQLiteHelper.WALKS_DESCRIPTION, description);
		values.put(SQLiteHelper.WALKS_TAGS, tagString);

		// Updating the database
		DataSource.database.update(SQLiteHelper.TABLE_WALKS, values, SQLiteHelper.COLUMN_ID + " = " + id, null);

		ContentValues fts3Values = new ContentValues();
		fts3Values.put(SQLiteHelper.SEARCH_WALK_NAME, name);
		fts3Values.put(SQLiteHelper.SEARCH_WALK_DESCRIPTION, description);
		fts3Values.put(SQLiteHelper.SEARCH_WALK_TAGS, tagString);

		DataSource.database.update(SQLiteHelper.TABLE_SEARCH, fts3Values,
				SQLiteHelper.SEARCH_WALK_ID + " = " + id, null);
	}

	/**
	 * Edits the note with the provided ID to have the content of the provided string.
	 * 
	 * @param id	The ID of the note to edit
	 * @param note	The new note text
	 */
	public static void editNote(long id, String note)
	{
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.NOTES_NOTE, note);

		DataSource.database.update(SQLiteHelper.TABLE_NOTES, values, SQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	// ///////////////////////////////////////////
	//
	// Delete methods
	//
	// ///////////////////////////////////////////
	/**
	 * Removes the provided Walk from the database.<br>
	 * <br>
	 * First deletes the Photos, GeoPoints and Notes for the walk, and the walk entry from the database,
	 * using a Thread so that it's run in the background and uses the UI thread as little as possible.
	 * Deletes the actual walk on the UI thread so that the walk cannot be interacted with once it's deleted.
	 * 
	 * @param walk	The Walk to be removed from the database.
	 */
	public static void deleteWalk(Walk walk)
	{
		final long id = walk.getId();

		Thread thread = new Thread() {
			@Override
			public void run()
			{
				deletePhotosForWalk(id);
				deleteGeoPointForWalk(id);
				deleteNotesForWalk(id);
				DataSource.database.delete(SQLiteHelper.TABLE_SEARCH, SQLiteHelper.SEARCH_WALK_ID + " = " + id,
						null);
			}
		};
		thread.start();

		// Removing the entry from the database
		DataSource.database.delete(SQLiteHelper.TABLE_WALKS, SQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * Removes the Notes for the provided walk from the database.
	 * 
	 * @param walk	The Walk from which the Notes should be removed from the database.
	 */
	public static void deleteNotesForWalk(long id)
	{
		DataSource.database.delete(SQLiteHelper.TABLE_NOTES, SQLiteHelper.NOTES_WALK_ID + " = " + id, null);
	}

	/**
	 * Removes the GeoPoints for the provided walk id from the database.
	 * 
	 * @param id	The Walk ID for which the GeoPoints should be removed from the database.
	 */
	private static void deleteGeoPointForWalk(long id)
	{
		DataSource.database.delete(SQLiteHelper.TABLE_GPS, SQLiteHelper.GPS_WALK_ID + " = " + id, null);
	}

	/**
	 * Removes the Photos for the provided walk from the database.
	 * 
	 * @param walk	The Walk from which the Photos should be removed from the database.
	 */
	public static void deletePhotosForWalk(long id)
	{
		DataSource.database.delete(SQLiteHelper.TABLE_PHOTOS, SQLiteHelper.PHOTOS_WALK_ID + " = " + id, null);
	}

	/**
	 * Removes the provided Note from the database.
	 * 
	 * @param id	The Note to be removed from the database.
	 */
	public static void deleteNote(Long id)
	{
		DataSource.database.delete(SQLiteHelper.TABLE_NOTES, SQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * Removes the provided Photo from the database.
	 * 
	 * @param id	The Photo to be removed from the database.
	 */
	public static void deletePhoto(long id)
	{
		DataSource.database.delete(SQLiteHelper.TABLE_PHOTOS, SQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	/**
	 * Removes the provided tags from all walks in the database.<br>
	 * <br>
	 * Uses the virtual search table to find all walks that match any of the selected tags. Then
	 * iterates through the Cursor, getting each Walk and the tags for that walk.<br>
	 * <br>
	 * It then uses ArrayList.removeAll to remove the Tags inside the checkedTags ArrayList from
	 * the walk's tag ArrayList, applies this updated ArrayList to the walk, and submits this
	 * change to the database.<br>
	 * <br>
	 * When the cursor has been iterated through, it then closes the cursor and ends.
	 * 
	 * @param checkedTags	The tags that should be removed.
	 */
	public static void deleteTags(ArrayList<Tag> checkedTags)
	{
		// Set up the query, using OR if there is more than one tag
		String query = SQLiteHelper.SEARCH_WALK_TAGS + " MATCH '";
		if (checkedTags.size() == 1)
		{
			query += checkedTags.get(0).getName() + "'";
		}
		else
		{
			for (int i = 0; i < checkedTags.size() - 1; i++)
			{
				query += checkedTags.get(i).getName() + " OR ";
			}
			query += checkedTags.get(checkedTags.size() - 1).getName() + "'";
		}

		// Retrieve all walks that match this from the database
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_SEARCH, DataSource.searchColumns, query,
				null, null, null, null);

		if (cursor == null) return;

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			// Get the walk from the cursor
			Walk walk = getWalkById(cursor.getLong(cursor.getColumnIndexOrThrow(SQLiteHelper.SEARCH_WALK_ID)));

			// Get the tags for the walk in array form and remove the supplied tags to be removed
			ArrayList<Tag> currentTags = walk.getTags();
			currentTags.removeAll(checkedTags);

			// Set up the walk's new tags and edit it in the database
			walk.setTags(currentTags);
			editWalk(walk);
			cursor.moveToNext();
		}

		// Close the cursor and return
		cursor.close();
	}

	// ///////////////////////////////////////////
	//
	// CursorTo... methods
	//
	// ///////////////////////////////////////////
	/**
	 * Converts the provided Cursor to a Walk and returns it
	 * 
	 * @param cursor	Contains the data needed to make a Walk object
	 * 
	 * @return			Returns the created Walk
	 */
	private static Walk cursorToWalk(Cursor cursor)
	{
		long id = cursor.getLong(0);
		String name = cursor.getString(1);
		String description = cursor.getString(2);
		String tags = cursor.getString(3);
		Long date = cursor.getLong(4);

		Walk walk = new Walk(id, name, description, date, Tag.stringArrayToList(tags.split(TAG_PADDING)));

		return walk;
	}

	/**
	 * Converts the provided Cursor to a Note and returns it
	 * 
	 * @param cursor	Contains the data needed to make a Note object
	 * 
	 * @return			Returns the created Note
	 */
	private static Note cursorToNote(Cursor cursor)
	{
		long id = cursor.getLong(0);
		double latitude = cursor.getDouble(2);
		double longitude = cursor.getDouble(3);
		String noteText = cursor.getString(4);

		Note note = new Note(id, latitude, longitude, noteText);

		return note;
	}

	/**
	 * Converts the provided Cursor to a GeoPoint and returns it
	 * 
	 * @param cursor	Contains the data needed to make a  object
	 * 
	 * @return				Returns the created 
	 */
	private static GeoPoint cursorToGeoPoint(Cursor cursor)
	{
		double latitude = cursor.getDouble(2);
		double longitude = cursor.getDouble(3);

		return new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
	}

	/**
	 * Converts the provided Cursor to a Photo and returns it
	 * 
	 * @param cursor	Contains the data needed to make a Photo object
	 * 
	 * @return			Returns the created Photo
	 */
	private static Photo cursorToPhoto(Cursor cursor)
	{
		long id = cursor.getLong(0);
		double latitude = cursor.getDouble(2);
		double longitude = cursor.getDouble(3);
		String file = cursor.getString(4);

		Photo photo = new Photo(id, latitude, longitude, file);
		return photo;
	}

	// ///////////////////////////////////////////
	//
	// Other methods
	//
	// ///////////////////////////////////////////
	/**
	 * Searches through the virtual search table for the provided query and returns all walks
	 * that match the query, sorted by the provided sort value.<br>
	 * <br>
	 * Sets up a sort String which contains either walk_id or name + ASC or DESC as appropriate.<br>
	 * <br>
	 * Then searches the search table for anything that matches the query. If null it returns null.
	 * Otherwise, it iterates through the Cursor, creating a Walk for each row and adding it to an
	 * ArrayList<Walk>. When done, it then closes the cursor and returns the walk list.
	 * 
	 * @param query		The string to search for
	 * @param sortValue	The sort value (date/name, ASC/DESC)
	 * 
	 * @return			An ArrayList of Walk objects that contain text matching the query
	 */
	public static ArrayList<Walk> search(String query, int sortValue)
	{
		// Set up the sort value
		String sort = "";
		switch (sortValue)
		{
		case CustomListActivity.DATE_ASCENDING:
			sort = SQLiteHelper.SEARCH_WALK_ID + " ASC";
			break;
		case CustomListActivity.DATE_DESCENDING:
			sort = SQLiteHelper.SEARCH_WALK_ID + " DESC";
			break;
		case CustomListActivity.NAME_ASCENDING:
			sort = SQLiteHelper.SEARCH_WALK_NAME + " ASC";
			break;
		case CustomListActivity.NAME_DESCENDING:
			sort = SQLiteHelper.SEARCH_WALK_NAME + " DESC";
			break;
		}

		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_SEARCH, DataSource.searchColumns,
				SQLiteHelper.TABLE_SEARCH + " MATCH '" + query + "'", null, null, null, sort);

		if (cursor == null) return null;

		ArrayList<Walk> walks = new ArrayList<Walk>();
		cursor.moveToFirst();

		while (!cursor.isAfterLast())
		{
			long id = cursor.getLong(cursor.getColumnIndex(SQLiteHelper.SEARCH_WALK_ID));
			walks.add(getWalkById(id));
			cursor.moveToNext();
		}
		cursor.close();
		return walks;
	}

	/**
	 * Stores a temporary walk (one the user is currently taking) in a permanent fashion.<br>
	 * <br>
	 * Used when the user saves an in-progress walk. Enters the provided walk  into the database. Uses the new
	 * version of the walk to get the database's row ID, and then updates all photos, notes and GPS values to
	 * have the saved walk ID as their walk_id foreign key. Then deletes the walk with ID 0 from the database.
	 * 
	 * @param receivedWalk	The walk the user has finished and wishes to save.
	 */
	public static void saveWalk(Walk receivedWalk)
	{
		// Retrieve the in-progress walk, name and description
		Walk walk = receivedWalk;
		String name = walk.getName();
		String description = walk.getDescription();

		// Convert the tags to String form, separated by the TAG_PADDING value
		String splitTags = "";
		ArrayList<Tag> tags = walk.getTags();
		Collections.sort(tags);
		Collections.sort(tags);
		if (tags.size() > 0)
		{
			for (int i = 0; i < tags.size() - 1; i++)
			{
				splitTags += tags.get(i).getName() + TAG_PADDING;
			}
			splitTags += tags.get(tags.size() - 1).getName();
		}

		// Creating the ContentValues and inserting the appropriate values
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.WALKS_NAME, name);
		values.put(SQLiteHelper.WALKS_DESCRIPTION, description);
		values.put(SQLiteHelper.WALKS_TAGS, splitTags);
		values.put(SQLiteHelper.WALKS_DATE, walk.getDate());

		// Inserting the saved into the database with a non-0 ID and retrieving it as a Cursor
		long insertId = DataSource.database.insert(SQLiteHelper.TABLE_WALKS, null, values);
		Cursor cursor = DataSource.database.query(SQLiteHelper.TABLE_WALKS, DataSource.walksColumns,
				SQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();

		// Converting the cursor to walk
		walk = cursorToWalk(cursor);
		cursor.close();

		// Insert the saved walk into the Search table
		ContentValues fts3Values = new ContentValues();
		fts3Values.put(SQLiteHelper.SEARCH_WALK_ID, insertId);
		fts3Values.put(SQLiteHelper.SEARCH_WALK_NAME, name);
		fts3Values.put(SQLiteHelper.SEARCH_WALK_DESCRIPTION, description);
		fts3Values.put(SQLiteHelper.SEARCH_WALK_TAGS, splitTags);

		DataSource.database.insert(SQLiteHelper.TABLE_SEARCH, null, fts3Values);

		// Prepare to update the GPS, photo and note tables
		ContentValues gpsValues = new ContentValues();
		gpsValues.put(SQLiteHelper.GPS_WALK_ID, insertId);

		ContentValues photoValues = new ContentValues();
		photoValues.put(SQLiteHelper.PHOTOS_WALK_ID, insertId);

		ContentValues noteValues = new ContentValues();
		noteValues.put(SQLiteHelper.NOTES_WALK_ID, insertId);

		// Update the GPS, Photo and Note tables
		DataSource.database.update(SQLiteHelper.TABLE_GPS, gpsValues, SQLiteHelper.GPS_WALK_ID + " = " + 0, null);
		DataSource.database.update(SQLiteHelper.TABLE_PHOTOS, photoValues,
				SQLiteHelper.PHOTOS_WALK_ID + " = " + 0, null);
		DataSource.database.update(SQLiteHelper.TABLE_NOTES, noteValues, SQLiteHelper.NOTES_WALK_ID + " = " + 0,
				null);

		// Removing the old entry from the database
		DataSource.database.delete(SQLiteHelper.TABLE_WALKS, SQLiteHelper.COLUMN_ID + " = " + 0, null);
	}

	/**
	 * Removes a temporary in-progress walk from the database, used when the user cancels a walk.<br>
	 * <br>
	 * Deletes the GPS, Photos and Notes for a walk, and then deletes the actual walk from the database.
	 */
	public static void cancelWalk()
	{
		// Deleting the GPS data and photos for this walk
		deleteGeoPointForWalk(0);
		deletePhotosForWalk(0);
		deleteNotesForWalk(0);

		// Removing the entry from the database
		DataSource.database.delete(SQLiteHelper.TABLE_WALKS, SQLiteHelper.COLUMN_ID + " = " + 0, null);
	}
}
