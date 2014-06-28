package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A custom dialogue asking the user if they wish to enable GPS (as it's currently disabled).<br>
 * <br>
 * If the user clicks yes, openGPSSettings() is called in the activity (which launches an Intent
 * to open the system GPS settings). If no, the dialogue is dismissed.<br>
 * 
 * @author Alex Hardwicke
 */
public class EnableGPSDialogue extends DialogFragment
{
	public static EnableGPSDialogue newInstance()
	{
		return new EnableGPSDialogue();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.gps_disabled_message)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						((WalkListActivity) getActivity()).openGPSSettings();
					}
				}).setNegativeButton(android.R.string.cancel, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_gps_off);
		String title = getString(R.string.gps_disabled_title);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
