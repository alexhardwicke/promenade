package com.digitalpies.promenade.walklist;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.PreferenceActivity;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Walk;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An implementation of CustomListActivity that is a searchable activity.
 * 
 * @author Alex Hardwicke
 */
public class SearchableActivity extends CustomListActivity
{
	private String query;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
		// Let the user tap the "home" button to go back to the main list
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setIcon(R.drawable.ic_menu_find);
		
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			String receivedQuery = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
			suggestions.saveRecentQuery(receivedQuery, null);
			this.query = receivedQuery;
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		updateWalksList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);
		return true;
	}

	@Override
	public void updateWalksList()
	{
		// Set the window title to the search query
		getActionBar().setSubtitle(getString(R.string.search_with_colon) + " " + this.query);
		
		// Get the sort value
		int checkedItem = this.preferences.getInt(PreferenceActivity.SORT_LIST, CustomListActivity.DATE_DESCENDING);

		// Retrieve the search result for the query
		this.walks = DataSource.search(this.query, checkedItem);
		
		// If null returned, create an empty ArrayList to prevent NullPointerExceptions
		if (this.walks == null)
			this.walks = new ArrayList<Walk>();
		
		// Set up the list
		this.adapter = new ListAdapter(this, this.walks);
		setListAdapter(this.adapter);
		this.adapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Intent intent;
		switch (item.getItemId())
		{
		case android.R.id.home:
			// "Up" icon in top left pressed. Return to the Walk List.
			intent = new Intent(this, com.digitalpies.promenade.walklist.WalkListActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.sort_button:
			// Let the user sort the list
			showSort();
			return true;
		case R.id.preferences_button:
			// Open preferences
			intent = new Intent(this, com.digitalpies.promenade.PreferenceActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
}
