package com.digitalpies.promenade.maps;

import java.util.ArrayList;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.digitalpies.promenade.database.Note;
import com.google.android.maps.OverlayItem;
import com.digitalpies.promenade.R;

/**
 * A custom implementation of CustomOverlayView that returns a custom view containing a TextView
 * inside a ScrollView for each note.
 * 
 * @author Alex Hardwicke
 */
public class CustomNoteView extends CustomOverlayView<OverlayItem>
{
	private NotePagerAdapter noteAdapter;
	private ArrayList<Note> notes;

	/**
	 * Standard constructor that just runs the super constructor.
	 * 
	 * @param context				The application context
	 * @param balloonBottomOffset	The offset from the bottom
	 */
	public CustomNoteView(Context context, int balloonBottomOffset, CustomMapActivity activity)
	{
		super(context, balloonBottomOffset, activity);
		this.notes = activity.getNotes();
	}

	/**
	 * Inflates the pager in the super method and sets up the note adapter
	 */
	@Override
	protected void setupView(Context context, final ViewGroup parent)
	{
		super.setupView(context, parent, R.layout.balloon_note_pager);

		this.noteAdapter = new NotePagerAdapter();
	}

	/**
	 * Setting the pager to have the correct adapter via the super method
	 */
	@Override
	protected void setBalloonData(OverlayItem item, ViewGroup parent)
	{
		super.setBalloonData(this.noteAdapter, this.notes.size(), item);
	}
	
	/**
	 * A custom PagerAdapter that displays the note text in a ScrollView.<br>
	 * 
	 * @author Alex Hardwicke
	 */
	private class NotePagerAdapter extends CustomPagerAdapter
	{
		@Override
		public int getCount()
		{
			return CustomNoteView.this.notes.size();
		}

		@Override
		public Object instantiateItem(View collection, int pagerPosition)
		{
			if (CustomNoteView.this.inflater == null) CustomNoteView.this.inflater = LayoutInflater.from(CustomNoteView.this.activity);
			
			// Retrieve the ScrollView and TextView
			ScrollView view = (ScrollView) CustomNoteView.this.inflater.inflate(R.layout.balloon_note_scroll, null);
			TextView textView = (TextView) view.findViewById(R.id.note_text);
			
			// Set up the text
			textView.setText(CustomNoteView.this.notes.get(pagerPosition).getNote());

			// Set the view to the scrollview
			((ViewPager) collection).addView(view, 0);

			return view;
		}
	}
}
