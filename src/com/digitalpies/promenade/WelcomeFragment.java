package com.digitalpies.promenade;

import com.digitalpies.promenade.R;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment view, opened from PreferenceActivity (detailed in preference_headers.xml).
 * 
 * Shows a Welcome/Tutorial page for the user if they need any help. The main view of the
 * fragment uses a ViewPager and custom PagerAdapter to inflate and show the appropriate xml
 * layout depending upon how the user has swiped the screen.
 * 
 * @author Alex Hardwicke
 */
public class WelcomeFragment extends Fragment
{
	private ViewPager welcomePager;
	private WelcomePagerAdapter welcomeAdapter;
	protected static int NUM_WELCOME_VIEWS = 3;
	private LayoutInflater inflater;
	
	@Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState)
	{
		this.inflater = layoutInflater;

		View contentView = this.inflater.inflate(R.layout.fragment_welcome, container, false);

		this.welcomeAdapter = new WelcomePagerAdapter();
		this.welcomePager = (ViewPager) contentView.findViewById(R.id.welcomepager);
		this.welcomePager.setAdapter(this.welcomeAdapter);
		
		return contentView;
	}

	private class WelcomePagerAdapter extends PagerAdapter
	{
		@Override
		public int getCount()
		{
			return NUM_WELCOME_VIEWS;
		}

		@Override
		public Object instantiateItem(View collection, int position)
		{
			// A new view needs to be created. Check the position and inflate the
			// appropriate layout.
			View view;
			if (position == 0)
			{
				view = WelcomeFragment.this.inflater.inflate(R.layout.fragment_welcome1, null, false);
			}
			else if (position == 1)
			{
				view = WelcomeFragment.this.inflater.inflate(R.layout.fragment_welcome2, null, false);
			}
			else
			{
				view = WelcomeFragment.this.inflater.inflate(R.layout.fragment_welcome3, null, false);
			}

			// Set the view in the viewpager to the retrieved view
			((ViewPager) collection).addView(view, 0);

			return view;
		}

		@Override
		public void destroyItem(View collection, int position, Object object)
		{
			((ViewPager) collection).removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			return (view == ((View) object));
		}
	}
}
