package com.digitalpies.promenade;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.dialogue.ResetToastsDialogue;
import com.digitalpies.promenade.walklist.CustomListActivity;

/**
 * PreferenceFragment that allows the user to customise application settings.<br>
 * <br>
 * Sets an onClickListener on the "reset help toasts" PreferenceScreen which shows
 * a dialogue asking the user to confirm when clicked.<br>
 * <br>
 * Creates a reference to the updateRate ListPreference so that the text can reflect
 * the update rate selected.<br>
 * 
 * @author Alex Hardwicke
 */
public class PreferenceFragmentApp extends PreferenceFragment
{
	SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Add the preferences
		addPreferencesFromResource(R.layout.preferences_app);

		// Get the shared preferences and references to the preference items
		this.preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final ListPreference updateRate = (ListPreference) findPreference("map_accuracy");
		PreferenceScreen resetPreferenceScreen = (PreferenceScreen) findPreference("reset_help_toasts");

		// Setting the onPreferenceChangeListener
		updateRate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue)
			{
				if (preference == (ListPreference) findPreference("map_accuracy"))
				{
					updateValue(preference, (String) newValue);
					return true;
				}
				return false;
			}
		});

		// Setting the onPreferenceClickListener
		resetPreferenceScreen.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0)
			{
				// Show confirm dialogue
				// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
				FragmentManager manager = getFragmentManager();
				if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_RESET_TOASTS) == null)
				{
					DialogFragment newFragment = ResetToastsDialogue.newInstance();
					newFragment.show(getFragmentManager(), CustomListActivity.DIALOGUE_FRAGMENT_RESET_TOASTS);
				}
				return true;
			}
		});
		
		// Make sure the updateRate text is already up to date
		String value = this.preferences.getString("map_accuracy", "20");
		updateValue(updateRate, value);
	}

	public void updateValue(Preference preference, String newValue)
	{
		// Update the text to reflect the chosen time
		String string = getString(R.string.map_accuracy) + " " + newValue + " "
				+ getString(R.string.map_accuracy_summary_suffix);
		preference.setSummary(string);
	}
}