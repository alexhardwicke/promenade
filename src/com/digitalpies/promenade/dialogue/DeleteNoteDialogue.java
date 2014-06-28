package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.maps.CustomMapActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Shows a dialogue asking the user if they are sure they wish to delete note. If they are, the note
 * position is passed to the map activity to be deleted.
 * 
 * @author Alex Hardwicke
 */
public class DeleteNoteDialogue extends DeleteAbstractDialogue
{	
	public static DeleteNoteDialogue newInstance(int position)
	{
		return new DeleteNoteDialogue(position);
	}
	
	public DeleteNoteDialogue()
	{
		super();
	}

	private DeleteNoteDialogue(int position)
	{
		super(position);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		super.onCreateDialog(savedInstanceState);
		
		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.cannot_be_undone)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						((CustomMapActivity) getActivity()).deleteNote(DeleteNoteDialogue.this.position);
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		String title = getString(R.string.delete_note);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
