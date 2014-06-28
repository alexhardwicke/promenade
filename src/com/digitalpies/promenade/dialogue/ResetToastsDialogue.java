package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.dialogue.WaitingForGPSDialogue;
import com.digitalpies.promenade.maps.NoteItemizedOverlay;
import com.digitalpies.promenade.maps.PhotoItemizedOverlay;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * This class shows a dialogue for the user asking them if they are sure they wish to reset the "help"
 * toasts. If they choose OK, it sets the values for all "first run" toasts to true.<br>
 * 
 * @author Alex Hardwicke
 */
public class ResetToastsDialogue extends DialogFragment
{
	public static ResetToastsDialogue newInstance()
	{
		return new ResetToastsDialogue();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.confirm_reset_toasts)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
						Editor editor = preferences.edit();
						editor.putBoolean(NoteItemizedOverlay.FIRST_NOTE_RUN, true);
						editor.putBoolean(PhotoItemizedOverlay.FIRST_PHOTO_RUN, true);
						editor.putBoolean(WaitingForGPSDialogue.FIRST_WAITING_RUN, true);
						editor.putBoolean(WalkListActivity.FIRST_LIST_RUN, true);
						editor.putBoolean(WalkDialogue.FIRST_WALK_RUN, true);
						editor.apply();
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		String title = getString(R.string.reset_toasts_dialogue);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
