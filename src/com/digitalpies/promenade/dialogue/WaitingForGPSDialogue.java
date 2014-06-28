package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * A custom progress dialogue telling the user the device is waiting for GPS to connect.
 * If the user clicks "okay", they cancel the GPS connection. Otherwise, the dialogue
 * is closed when GPS connects.<br>
 * <br>
 * Shows a toast telling the user they can press home and the app will begin the walk
 * when GPS locks on.
 * 
 * @author Alex Hardwicke
 */
public class WaitingForGPSDialogue extends DialogFragment
{
	public static final String FIRST_WAITING_RUN = "FIRST_WAITING_RUN";

	public static WaitingForGPSDialogue newInstance()
	{
		return new WaitingForGPSDialogue();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{		
		// Creating and setting up the dialogue
		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(getString(R.string.connecting_to_gps));

		// Setting up a negative button to cancel GPS if the user presses it.
		dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface receivedDialog, int buttonId)
					{
						receivedDialog.cancel();
					}
				});

		// Hijack menu and search keys to prevent the dialogue being dismissed by search being opened.
		dialog.setOnKeyListener(new CustomOnKeyListener());
		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_gps_connecting);
		String title = getString(R.string.waiting_for_gps);

		// Get the SharedPreferences and check if this is the first time the user is at this dialogue. If so, inform
		// them with a Toast that they can browse away, and set the preferences so that this can't happen again
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		if (preferences.getBoolean(FIRST_WAITING_RUN, true))
		{
			Toast.makeText(getActivity(), R.string.toast_can_browse_away_from_gps, Toast.LENGTH_LONG).show();
			Editor editor = preferences.edit();
			editor.putBoolean(FIRST_WAITING_RUN, false);
			editor.apply();
		}

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, dialog);
	}

	/**
	 * Run when the dialog is cancelled by tapping outside of the dialogue or via the cancel button.
	 * Cancels the GPS connection attempt.
	 * 
	 * @param dialog	The dialog being cancelled.
	 */
	@Override
	public void onCancel(DialogInterface dialog)
	{
		((WalkListActivity) getActivity()).cancelGPS();
		super.onCancel(dialog);
	}
}
