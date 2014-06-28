package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.PreferenceActivity;
import com.digitalpies.promenade.walklist.CustomListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Shows a dialogue allowing the user to sort the list by name or date, ascending or descending.<br>
 * <br>
 * Stores this status in the app preferences because this should be kept between application runs.<br>
 * <br>
 * When an option is chosen, it saves it in the app's preferences, updates the list the user is viewing,
 * and dismisses itself.
 * 
 * @author Alex Hardwicke
 */
public class SortListDialogue extends DialogFragment
{
	public static SortListDialogue newInstance()
	{
		return new SortListDialogue();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final int checkedItem = preferences.getInt(PreferenceActivity.SORT_LIST, CustomListActivity.DATE_DESCENDING);

		AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
		// Create the radio buttons, passing in a String array of values, and the currently chosen sort order
				.setSingleChoiceItems(getResources().getStringArray(R.array.sort_list), checkedItem,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								SharedPreferences.Editor editor = preferences.edit();
								editor.putInt(PreferenceActivity.SORT_LIST, which);
								editor.apply();
								((CustomListActivity) getActivity()).updateWalksList();
								dismiss();
							}
						}).setNegativeButton(android.R.string.cancel, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_sort);
		String title = getString(R.string.sort);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
