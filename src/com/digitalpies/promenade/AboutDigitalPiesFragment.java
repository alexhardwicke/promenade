package com.digitalpies.promenade;

import com.digitalpies.promenade.R;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment view, opened from PreferenceActivity (detailed in preference_headers.xml).
 * 
 * Shows a simple About page with information on the DigitalPies. Three TextViews are
 * retrieved and have onClick listeners attached to start the appropriate Intent.
 * 
 * @author Alex Hardwicke
 */
public class AboutDigitalPiesFragment extends Fragment
{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Expand the layout and get the three textViews that need to be clickable.
		View contentView = inflater.inflate(R.layout.activity_about, container, false);
		TextView websiteTextView = (TextView) contentView.findViewById(R.id.website);
		TextView emailTextView = (TextView) contentView.findViewById(R.id.email);
		TextView twitterTextView = (TextView) contentView.findViewById(R.id.twitter);

		// When clicked, launch a web intent for http://www.digitalpies.com
		websiteTextView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent webIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.digitalpies.com"));
				startActivity(webIntent);
			}
		});

		// When clicked, launch an e-mail intent for digitalpies@digitalpies.com with the
		// subject being the app's name.
		emailTextView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                
                emailIntent.setType("plain/text");

                String[] email = new String[] { getResources().getString(R.string.email) };
                String subject = getResources().getString(R.string.app_name);
                
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, email);
         
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
         
              startActivity(emailIntent);
			}
		});

		// When clicked, launch a web intent for http://www.twitter.com/DigitalPies
		twitterTextView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent twitterIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://www.twitter.com/DigitalPies"));
				startActivity(twitterIntent);
			}
		});
		
		return contentView;
	}
}
