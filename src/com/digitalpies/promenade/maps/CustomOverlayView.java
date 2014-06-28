package com.digitalpies.promenade.maps;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;
import com.digitalpies.promenade.R;
import com.viewpagerindicator.CirclePageIndicator;

/**
 * An abstract extension of BalloonOverlayView which handles the methods both have to perform, and includes
 * implementations of a CustomOnPageListener and partially a ViewPager (for the methods that are the same
 * for all sub-implementations).
 * 
 * @author Alex Hardwicke
 */
public abstract class CustomOverlayView<Item extends OverlayItem> extends BalloonOverlayView<OverlayItem>
{
	protected ViewPager pager;
	protected CirclePageIndicator indicator;
	protected CustomMapActivity activity;
	protected LayoutInflater inflater = null;

	public CustomOverlayView(Context context, int balloonBottomOffset, CustomMapActivity activity)
	{
		super(context, balloonBottomOffset);
		this.activity = activity;
	}
	
	protected void setupView(Context context, final ViewGroup parent, int id)
	{
		if (this.inflater == null) this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View v = this.inflater.inflate(id, parent);
		this.pager = (ViewPager) v.findViewById(R.id.pager);
		this.indicator = (CirclePageIndicator) v.findViewById(R.id.indicator);
	}
	
	public void setBalloonData(PagerAdapter adapter, int size, OverlayItem item)
	{
		this.pager.setAdapter(adapter);

		// If there's more than one item and less than 22, show a scroll position indicator
		// (Less than 22 because 22+ expands out of the bubble)
		if (size > 1 && size < 22)
		{
			this.indicator.setViewPager(this.pager);
			this.indicator.setOnPageChangeListener(new CustomOnPageChangeListener());
			this.indicator.setCurrentItem(Integer.parseInt(item.getTitle()));
			this.pager.setCurrentItem(Integer.parseInt(item.getTitle()));
		}
		else
		{
			this.indicator.setVisibility(View.GONE);
			this.pager.setOnPageChangeListener(new CustomOnPageChangeListener());
			this.pager.setCurrentItem(Integer.parseInt(item.getTitle()));
		}
	}
	
	/**
	 * A custom implementation of an OnPageChangeListener.<br>
	 * <br>
	 * Whenever the page is scrolled, keeps the activity up to date with which item is selected.
	 * 
	 * @author Alex Hardwicke
	 */
	protected class CustomOnPageChangeListener implements OnPageChangeListener
	{
		@Override
		public void onPageScrollStateChanged(int arg0)
		{
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2)
		{
			if (CustomOverlayView.this.activity != null)
				CustomOverlayView.this.activity.setSelected(CustomOverlayView.this.pager.getCurrentItem());
		}

		@Override
		public void onPageSelected(int arg0)
		{
		}
	}

	/**
	 * A custom PagerAdapter that implements a few methods that are the same in both subclasses.<br>
	 * 
	 * @author Alex Hardwicke
	 */
	protected abstract class CustomPagerAdapter extends PagerAdapter
	{
		@Override
		public void destroyItem(View collection, int pagerPosition, Object object)
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
