package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.maps.CustomMapActivity;
import com.digitalpies.promenade.maps.MapViewActivity;
import com.digitalpies.promenade.maps.MapWalkActivity;
import com.digitalpies.promenade.walklist.CustomListActivity;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class is a custom listener used for WalkDialogue.<br>
 * <br>
 * It prevents the dialogue being automatically closed when a button is clicked, and instead only
 * dismisses the dialogue on OK if a name has been entered. If a name has been entered, it runs
 * various methods depending upon the task and source Activity.<br>
 * 
 * @author Alex Hardwicke
 */
public class WalkOnShowListener implements DialogInterface.OnShowListener
{
	protected AlertDialog alertDialog;
	protected Activity activity;
	protected String task;
	protected Walk walk;
	protected ArrayList<Tag> checkedTagsList;

	/**
	 * @param alertDialog		The dialog that created this one.
	 * @param activity			The original activity that created the first dialogue.
	 * @param task				The task (New vs edit)
	 * @param walk				The walk that is being edited
	 * @param checkedTagsList	The ArrayList that holds the checked tags
	 */
	public WalkOnShowListener(AlertDialog alertDialog, Activity activity, String task, Walk walk,
			ArrayList<Tag> checkedTagsList)
	{
		this.alertDialog = alertDialog;
		this.activity = activity;
		this.task = task;
		this.walk = walk;
		this.checkedTagsList = checkedTagsList;
	}

	@Override
	public void onShow(DialogInterface dialog)
	{
		// Get the buttons and the two EditTexts
		Button okButton = this.alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		final EditText nameEditText = (EditText) this.alertDialog.findViewById(R.id.walk_name);
		final EditText descriptionEditText = (EditText) this.alertDialog.findViewById(R.id.walk_description);
		final EditText tagsEditText = (EditText) this.alertDialog.findViewById(R.id.walk_tags);

		// Name field is empty, show keyboard if no physical keyboard ready
		if (nameEditText.getText().length() == 0)
		{
			nameEditText.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) this.activity
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT);
		}

		// Add a listener to the OK button
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				String name = nameEditText.getText().toString();
				String description = descriptionEditText.getText().toString();

				// If no name, show a Toast telling the user they must enter one.
				if (name.length() == 0)
					Toast.makeText(WalkOnShowListener.this.activity, R.string.confirm_enter_name, Toast.LENGTH_SHORT).show();
				else
				{
					// Retrieve tags from the tagEditText, if any, and add them to the checked tags if they're
					// not already in
					ArrayList<Tag> newTags = WalkDialogue.newTags(tagsEditText);

					for (Tag tag : newTags)
						if (!WalkOnShowListener.this.checkedTagsList.contains(tag))
							WalkOnShowListener.this.checkedTagsList.add(tag);

					// Start a new walk
					if (WalkOnShowListener.this.task.equals(CustomListActivity.NEW_WALK_TASK))
					{
						// Insert the walk into the database
						Walk createdWalk = DataSource.createTemporaryWalk(name, description,
								WalkOnShowListener.this.checkedTagsList);

						// If GPS is locked, start GPS tracking and open MapWalkActivity
						if (((WalkListActivity) WalkOnShowListener.this.activity).getGPSLocked())
						{
							((WalkListActivity) WalkOnShowListener.this.activity).startGPSTracking();
							Intent intent = new Intent(WalkOnShowListener.this.activity,
									com.digitalpies.promenade.maps.MapWalkActivity.class);
							intent.putExtra(WalkListActivity.WALK_TAG, createdWalk);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							WalkOnShowListener.this.activity.startActivity(intent);
						}
						// If GPS isn't locked, show a WaitingForGPSDialogue.
						else
						{
							// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the
							// button)
							FragmentManager manager = WalkOnShowListener.this.activity.getFragmentManager();
							if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_WAITING_FOR_GPS) == null)
							{
								DialogFragment newFragment = WaitingForGPSDialogue.newInstance();
								newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_WAITING_FOR_GPS);
							}
						}
					}
					// Save the chosen walk
					else
					{
						// Updates the current walk
						WalkOnShowListener.this.walk.setName(name);
						WalkOnShowListener.this.walk.setDescription(description);
						WalkOnShowListener.this.walk.setTags(WalkOnShowListener.this.checkedTagsList);

						// Sends the call to the correct class
						if (WalkOnShowListener.this.task.equals(CustomMapActivity.SAVE_WALK_TASK))
							((MapWalkActivity) WalkOnShowListener.this.activity).saveWalk(WalkOnShowListener.this.walk);
						else if (WalkOnShowListener.this.task.equals(CustomListActivity.EDIT_WALK_TASK))
						{
							// Sends the call to the correct class
							if (WalkOnShowListener.this.activity instanceof CustomListActivity)
								((CustomListActivity) WalkOnShowListener.this.activity)
										.editWalk(WalkOnShowListener.this.walk);
							else
								((MapViewActivity) WalkOnShowListener.this.activity).editWalk(WalkOnShowListener.this.walk);
						}
					}
					// Edit the chosen walk
					WalkOnShowListener.this.alertDialog.dismiss();
				}
			}
		});
	}
}
