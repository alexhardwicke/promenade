package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.maps.CustomMapActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * This class shows a dialogue for the user asking them if they are sure they wish to delete a photo.<br>
 * <br>
 * It also shows a checkbox asking if they wish to delete the photo from the phone's memory as well.
 * 
 * @author Alex Hardwicke
 */
public class DeletePhotoDialogue extends DeleteAbstractDialogue
{
	protected boolean selected;

	public final static String SELECTED = "SELECTED";

	public static DeletePhotoDialogue newInstance(int position)
	{
		return new DeletePhotoDialogue(position);
	}

	public DeletePhotoDialogue()
	{
		super();
	}

	private DeletePhotoDialogue(int position)
	{
		super(position);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putBoolean(SELECTED, this.selected);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final String[] options = new String[1];
		options[0] = getString(R.string.delete_photos_long);

		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
			this.selected = savedInstanceState.getBoolean(SELECTED);

		// Get and inflate the layout, and then the checkBox
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.dialogue_delete_photo, null);

		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.delete_photo);

		// Restore selected status and add a listener
		checkBox.setChecked(this.selected);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1)
			{
				DeletePhotoDialogue.this.selected = checkBox.isChecked();
			}

		});

		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						// Delete the photo
						((CustomMapActivity) getActivity()).deletePhoto(DeletePhotoDialogue.this.position, DeletePhotoDialogue.this.selected);
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		String title = getString(R.string.delete_photos);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
