package com.digitalpies.promenade;

import com.digitalpies.promenade.R;

import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * A fragment view, opened from PreferenceActivity (detailed in preference_headers.xml).
 * 
 * Shows a simple About page with information on the app. The version PreferenceScreens is
 * retrieved and has the version number set as the summary.
 * 
 * @author Alex Hardwicke
 */
public class AboutFragment extends PreferenceFragment
{
	SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Add the preferences
		addPreferencesFromResource(R.layout.preferences_about);

		// Get the shared preferences and references to the preference items
		PreferenceScreen appVersionPreferenceScreen = (PreferenceScreen) findPreference("app_version");

		try
		{
			String appVersion = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
			appVersionPreferenceScreen.setSummary(appVersion);
		}
		catch (NameNotFoundException e)
		{
		}
	}
}