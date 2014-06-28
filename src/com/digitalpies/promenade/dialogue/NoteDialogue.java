package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Shows a dialogue with an EditText. Can be used to create or edit a note.<br>
 * <br>
 * If editing, the EditText already contains the note which the user has chosen to edit.<br>
 * <br>
 * Implements a custom OnShowListener to prevent the user from saving a note with no text in.
 * 
 * @author Alex Hardwicke
 */
public class NoteDialogue extends DialogFragment
{
	private String userText = "";
	protected long id;
	protected boolean newNote = true;

	private static final String BOOL = "BOOL";
	private static final String ID = "ID";
	private static final String TEXT = "TEXT";

	public static NoteDialogue newInstance()
	{
		return new NoteDialogue();
	}

	public static NoteDialogue newInstance(Long id, String userText)
	{
		return new NoteDialogue(id, userText);
	}

	public NoteDialogue()
	{
		super();
	}

	private NoteDialogue(Long id, String userText)
	{
		this.id = id;
		this.userText = userText;
		this.newNote = false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(TEXT, this.userText);
		outState.putLong(ID, this.id);
		outState.putBoolean(BOOL, this.newNote);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Get the layout and EditText
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.dialogue_add_note, null);
		final EditText editText = (EditText) view.findViewById(R.id.note_text);

		// Get the details and populate the EditText
		if (savedInstanceState != null)
		{
			this.userText = savedInstanceState.getString(TEXT);
			this.id = savedInstanceState.getLong(ID);
			this.newNote = savedInstanceState.getBoolean(BOOL);
		}

		editText.setText(this.userText);

		// Set up the EditText
		editText.requestFocus();
		InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

		// Creating and returning the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)
				.setPositiveButton(android.R.string.ok, null).setNegativeButton(android.R.string.cancel, null).create();
		alertDialog.setOnShowListener(new NoteOnShowListener(alertDialog, getActivity(), this.newNote, this.id));

		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_pin);
		String title = getString(R.string.take_note);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
