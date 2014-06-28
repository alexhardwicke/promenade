package com.digitalpies.promenade.dialogue;

import java.text.DateFormat;
import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;
import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * A custom dialogue showing the user the details of the provided walk.<br>
 * <br>
 * Calculates the distance of the walk by calculating the distance from each GeoPoint
 * to the next, and stores the result in two doubles (miles and kilometres), both
 * rounded to 1 decimal place. This is done when the dialogue is first created and
 * then saved, so that it doesn't need to be recalculated if the dialogue is
 * re-created (e.g. on an orientation change).
 * 
 * @author Alex Hardwicke
 */
public class WalkDetailsDialogue extends DialogFragment
{	
	private static final String WALK = "WALK";
	private static final String DISTANCES = "DISTANCES";
	
	private double[] distances = new double[2];
	private Walk walk;
	
	public static WalkDetailsDialogue newInstance(Walk walk)
	{
		return new WalkDetailsDialogue(walk);
	}
	
	public WalkDetailsDialogue()
	{
		super();
	}

	public WalkDetailsDialogue(Walk walk)
	{
		super();
		this.walk = walk;
		this.distances = calculateDistance(DataSource.getGeoPointsForWalk(walk));
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putDoubleArray(DISTANCES, this.distances);
		outState.putParcelable(WALK, this.walk);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			this.walk = savedInstanceState.getParcelable(WALK);
			this.distances = savedInstanceState.getDoubleArray(DISTANCES);
		}

		// Inflate the layout and retrieve the four textviews
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.dialogue_walk_details, null);
		
		TextView description = (TextView) view.findViewById(R.id.walk_details_description);
		TextView tags = (TextView) view.findViewById(R.id.walk_details_tags);
		TextView dateTime = (TextView) view.findViewById(R.id.walk_details_date_time);
		TextView distance = (TextView) view.findViewById(R.id.walk_details_distance);

		// Description. If the length is greater than 0, set the text.
		String descriptionString = this.walk.getDescription();
		if (descriptionString.length() > 0)
		{
			description.setText(descriptionString);
		}
		// If no description, hide the description header and description textview
		else
		{
			((TextView) view.findViewById(R.id.walk_details_description_header)).setVisibility(View.GONE);
			description.setVisibility(View.GONE);
		}
		
		// Tags. Get the tags, and if the list is greater than 0, iterate through the ArrayList, adding each
		// tag to a String, and then applying the text to the textview
		ArrayList<Tag> tagList = this.walk.getTags();
		if (tagList.size() > 0)
		{
			String tagString = "";
				for (int i = 0; i < tagList.size() - 1; i++)
				{
					tagString += tagList.get(i).getName() + ", ";
				}
			tagString += tagList.get(tagList.size()-1).getName();
			tags.setText(tagString);
		}
		// If no tags, hide the tags header and tags textview
		else
		{
			((TextView) view.findViewById(R.id.walk_details_tags_header)).setVisibility(View.GONE);
			tags.setVisibility(View.GONE);
		}

		// Use the DateFormat class to get the default date_time value for the walk's starting time.
		dateTime.setText(DateFormat.getDateTimeInstance().format(this.walk.getDate()));

		// Use the distance values calculated in the once-run constructor to create a string and set it
		String distanceString = this.distances[0] + " " + getString(R.string.miles) + " (" + this.distances[1] + " km)";
		distance.setText(distanceString);

		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)
				.setNeutralButton(android.R.string.ok, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_info);
		String title = this.walk.getName();

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}

	/**
	 * Calculates the total distance amongst an ArrayList of GeoPoints.<br>
	 * <br>
	 * This is called in the constructor that takes parameters (i.e. the constructor called
	 * when the dialogue is first created, not when it is re-created on an orientation change).
	 * This saves processing time for the user.<br>
	 * <br>
	 * Uses Location objects to calculate the distance from each geoPoint to the next, adding
	 * the result to a double. After this, the result (in metres) is used to calculate the
	 * distance in kilometres (divide by 1000), rounded to 1 d.p. The result is also used to
	 * calculate the distance in miles (multiply by 0.000625, which is the same as dividing by
	 * 1000 (to get to kilometres), and then multiplying by 5/8 (kilometres to miles).<br>
	 * 
	 * @param geoPoints	The walk's ArrayList of GeoPoint objects
	 * 
	 * @return	A two dimensional array containing the distance of the walk in miles and kilometres.
	 */
	private static double[] calculateDistance(ArrayList<GeoPoint> geoPoints)
	{
		double distance = 0;
		Location locationStart = new Location("START");
		Location locationEnd = new Location("END");
		locationEnd.setLatitude(geoPoints.get(0).getLatitudeE6() / 1E6);
		locationEnd.setLongitude(geoPoints.get(0).getLongitudeE6() / 1E6);
		for (GeoPoint geoPoint : geoPoints)
		{
			// Copy locationEnd over so that only one has to be calculated
			locationStart.setLatitude(locationEnd.getLatitude());
			locationStart.setLongitude(locationEnd.getLongitude());
			
			locationEnd = new Location("END");
			locationEnd.setLatitude(geoPoint.getLatitudeE6() / 1E6);
			locationEnd.setLongitude(geoPoint.getLongitudeE6() / 1E6);
			distance += locationStart.distanceTo(locationEnd);
		}
		
		double kilometers = roundDouble(distance/1000);
		double miles = roundDouble(distance * 0.000625);
		
		double[] doubleArray = { miles, kilometers };
		
		return doubleArray;
	}
	
	/**
	 * Rounds a double to one decimal place.
	 * 
	 * @param inDouble	The double to be rounded
	 * 
	 * @return	The double to 1 decimal place (#.#)
	 */
	private static double roundDouble(Double inDouble)
	{
		double result = inDouble * 10;
		result = Math.round(result);
		return result / 10;
	}
}
