package com.digitalpies.promenade.walklist;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * ListAdapter is an adapter used by CustomListActivity (and as such, WalkListActivity and SearchableActivity).
 * It takes in an ArrayList<Walk>, which contains all of the walks that are going to be displayed, and then
 * processes them so that they are displayed on the ListView.
 * 
 * @author Alex Hardwicke
 */
public class ListAdapter extends BaseAdapter
{
	private final Context context;
	private ArrayList<Walk> walks;
	private LayoutInflater inflater;
	
	public ListAdapter(Context context, ArrayList<Walk> newWalks)
	{
		super();
		this.context = context;
		this.walks = new ArrayList<Walk>();

		this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		// Don't show walk ID 0 (in-progress walk)
		for (Walk walk : newWalks)
		{
			if (walk.getId() != 0) 
				this.walks.add(walk);
		}
	}
	
	public void setWalks(ArrayList<Walk> newWalks)
	{
		this.walks = new ArrayList<Walk>();

		// Don't show walk ID 0 (in-progress walk)
		for (Walk walk : newWalks)
		{
			if (walk.getId() != 0) 
				this.walks.add(walk);
		}
	}

	/**
	 * Populates the ListView with the correct objects from the walks ArrayList<Walk>
	 * 
	 * @param position 		<i>int</i> 			The position in the ListView of the item being processed
	 * @param convertView 	<i>View</i> 		A view to be re-used if possible
	 * @param parent 		<i>ViewGroup</i>	unused
	 * 
	 * @return 					A View for the supplied position
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		// Retrieve the walk for the current position and the name, date and tags for that walk
		Walk walk = this.walks.get(position);
		String name = walk.getName();
		Long dateLong = walk.getDate();
		String description = walk.getDescription();
		ArrayList<Tag> tagList = walk.getTags();
		String tags = "";
		if (tagList.size() > 0)
		{
			for (int i = 0; i < tagList.size()-1; i++)
				tags += (tagList.get(i).getName()) + ", ";
			tags += tagList.get(tagList.size() - 1).getName();
		}
		String dateString = getDate(dateLong);
		
		if (convertView == null)
		{
			// Get a LayoutInflater and use it to inflate the layout xml
			convertView = this.inflater.inflate(R.layout.list_walk_list_view, parent, false);
		}

		// Retrieve the three TextViews for the data
		TextView nameTextView = (TextView) convertView.findViewById(R.id.name_text_view);
		TextView dateTextView = (TextView) convertView.findViewById(R.id.date_text_view);
		TextView descriptionTextView = (TextView) convertView.findViewById(R.id.description_text_view);
		TextView tagsTextView = (TextView) convertView.findViewById(R.id.tags_text_view);

		// Set the TextViews to contain the appropriate data
		nameTextView.setText(name);
		dateTextView.setText(dateString);
		descriptionTextView.setText(description);
		tagsTextView.setText(tags);

		return convertView;
	}

	@Override
	public int getCount()
	{
		return this.walks.size();
	}

	@Override
	public Object getItem(int position)
	{
		return this.walks.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return this.walks.get(position).getId();
	}
	
	public static String getDate(long dateLong)
	{
		// Create two Date objects - one for the walk time (using the value provided),
		// and one for 00:00 this morning generated by a Calendar object.
		Date date = new Date(dateLong);
		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date midnightDate = calendar.getTime();
		
		// If the walk date happened before 00:00 this morning, show format:
		// MMM DD (e.g. Jun 07)
		// If the walk date happened after this time (i.e. today), show format:
		// hh:mm (e.g. 12:38)
		DateFormat dateFormat;
		if (date.before(midnightDate))
			dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
		else
			dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

		return dateFormat.format(dateLong);
	}
}
