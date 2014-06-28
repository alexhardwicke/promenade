package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.maps.MapWalkActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * A custom dialogue asking the user if they are sure they wish to cancel a walk. Used only when a walk is
 * in progress, by MapWalkActivity. When first created, takes in a selected Walk as a parameter. This walk
 * is saved between instances (e.g. when the screen rotates) using onSaveInstanceState.<br>
 * <br>
 * If the user chooses yes, it runs "finishWalk" in the activity. The dialogue is dismissed on cancel.<br>
 * <br>
 * 
 * @author Alex Hardwicke
 */
public class CancelWalkDialogue extends DialogFragment
{
	private boolean hasPhotos;
	private boolean selected;
	
	private final static String HAS_PHOTOS = "HAS_PHOTOS";
	private final static String SELECTED = "SELECTED";
	
	public static CancelWalkDialogue newInstance(boolean hasPhotos)
	{
		return new CancelWalkDialogue(hasPhotos);
	}
	
	public CancelWalkDialogue()
	{
		super();
	}
	
	private CancelWalkDialogue(boolean hasPhotos)
	{
		super();
		this.hasPhotos = hasPhotos;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean(HAS_PHOTOS, this.hasPhotos);
		outState.putBoolean(SELECTED, this.selected);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			this.hasPhotos = savedInstanceState.getBoolean(HAS_PHOTOS);
			this.selected = savedInstanceState.getBoolean(SELECTED);
		}

		// Inflate the layout
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.dialogue_delete_walk, null);
		
		// Retrieve the checkbox
		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.delete_photos);

		// If there is at least one photo
		if (this.hasPhotos)
		{
			// Restore the checkboxes selected status (false unless set before a rotation)
			checkBox.setChecked(this.selected);
			
			// Set an onCheckedChangeListener to keep a boolean aware of the checked status
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1)
				{
					CancelWalkDialogue.this.selected = checkBox.isChecked();
				}

			});
		}
		// No photos hide the checkbox from the dialogue
		else
			checkBox.setVisibility(View.GONE);

		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						((MapWalkActivity) getActivity()).cancelWalk(CancelWalkDialogue.this.selected);
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		String title = getString(R.string.cancel_walk_title);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
