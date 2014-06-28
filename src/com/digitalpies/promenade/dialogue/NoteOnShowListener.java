package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.maps.CustomMapActivity;
import com.digitalpies.promenade.maps.MapWalkActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class is a custom listener used by the NoteDialogue class.<br>
 * <br>
 * It prevents the dialogue being automatically closed, and instead only dismisses the dialogue if a note has been
 * entered.<br>
 * <br>
 * If a note has been entered, it performs different commands based upon the source Activity.
 * 
 * @author Alex Hardwicke
 */
public class NoteOnShowListener implements DialogInterface.OnShowListener
{
	protected AlertDialog alertDialog;
	protected Activity activity;
	protected boolean newNote;
	protected long id;

	public NoteOnShowListener(AlertDialog alertDialog, Activity activity, boolean newNote, long id)
	{
		this.alertDialog = alertDialog;
		this.activity = activity;
		this.newNote = newNote;
		this.id = id;
	}

	@Override
	public void onShow(DialogInterface dialog)
	{
		// Get the button and the three EditTexts
		Button button = this.alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		final EditText noteEditText = (EditText) this.alertDialog.findViewById(R.id.note_text);

		// Note field is empty (new note), show keyboard if no physical keyboard ready
		if (noteEditText.getText().toString().length() == 0)
		{
			noteEditText.requestFocus();
			InputMethodManager inputMethodManager = (InputMethodManager) this.activity
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.showSoftInput(noteEditText, InputMethodManager.SHOW_IMPLICIT);
		}

		// Add a custom listener to the button, using the EditText data.
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				String noteText = noteEditText.getText().toString();
				// If no note, show a Toast.
				if (noteText.length() == 0)
					Toast.makeText(NoteOnShowListener.this.activity, R.string.confirm_enter_note, Toast.LENGTH_SHORT).show();
				else
				{
					// If a new note, send it to MapWalkActivity.
					if (NoteOnShowListener.this.newNote)
						((MapWalkActivity) NoteOnShowListener.this.activity).newNote(noteEditText.getText().toString());
					// Else, update it in either map class
					else
						((CustomMapActivity) NoteOnShowListener.this.activity).updateNote(NoteOnShowListener.this.id,
								noteEditText.getText().toString());
					// Dismiss the dialogue
					NoteOnShowListener.this.alertDialog.dismiss();
				}
			}
		});
	}
}
