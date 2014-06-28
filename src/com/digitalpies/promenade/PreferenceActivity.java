package com.digitalpies.promenade;

import java.util.List;

import com.digitalpies.promenade.R;

import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Displays a list of preference headers from R.layout.preference_headers which allows the
 * user to view preference Fragments.
 * 
 * @author Alex Hardwicke
 */
public class PreferenceActivity extends android.preference.PreferenceActivity
{
	public final static String SORT_LIST = "SORT_LIST";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Setting the action bar icon to be "up" here so that it automatically applies
		// to all fragments (on phones)
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Setting the ActionBar backgrounds
		BitmapDrawable bitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.stripes);
		bitmap.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		getActionBar().setBackgroundDrawable(bitmap);
		getActionBar().setSplitBackgroundDrawable(getResources().getDrawable(R.drawable.purple_dot));
		
	}
	
	@Override
	public void onBuildHeaders(List<Header> target)
	{
		loadHeadersFromResource(R.layout.preference_headers, target);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			// Use finish() to return to the previous activity. This is safe because
			// PreferenceActivity must always have another activity behind it.
			finish();
			return true;
		}
		return false;
	}
}