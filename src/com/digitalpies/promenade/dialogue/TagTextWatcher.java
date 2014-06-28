package com.digitalpies.promenade.dialogue;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Used to keep all text entered in an EditText lower case. Whenever text is edited, it checks to see if the text
 * String is equal to String.toLowerCase(). If it is, it returns - this prevents an infinite loop. If not, it
 * replaces the String with the lower case version and moves the cursor to the position it was at before.
 * 
 * @author Alex Hardwicke
 */
public class TagTextWatcher implements TextWatcher
{
	private EditText tagsEditText;

	public TagTextWatcher(EditText tagsEditText)
	{
		this.tagsEditText = tagsEditText;
	}

	public void setTags(EditText tagsEditText)
	{
		this.tagsEditText = tagsEditText;
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
		// If selectionstart != selectionend, then the user has text selected. Don't do anything right now!
		if (this.tagsEditText.getSelectionStart() != this.tagsEditText.getSelectionEnd()) return;

		// Get the position of the cursor. Used to put the cursor at the right spot at the end.
		int cursorPosition = this.tagsEditText.getSelectionStart();

		// Getting the text. If it is identical in lower and upper case, return.
		String text = this.tagsEditText.getText().toString();
		if (text.equals(text.toLowerCase())) return;

		// Otherwise, make it lower case and the move the cursor to where it was before.
		this.tagsEditText.setText(text.toLowerCase());
		Selection.setSelection(this.tagsEditText.getText(), cursorPosition);
	}

	@Override
	public void afterTextChanged(Editable arg0)
	{
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
	{
	}
}
