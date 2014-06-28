package com.digitalpies.promenade.dialogue;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * This abstract class is used by the Note & Photo dialogues. This class manages keeping track of the
 * item's position value.
 * 
 * @author Alex Hardwicke
 */
public abstract class DeleteAbstractDialogue extends DialogFragment
{
	protected int position;

	public final static String POSITION = "POSITION";

	public DeleteAbstractDialogue()
	{
		super();
	}

	protected DeleteAbstractDialogue(int position)
	{
		super();
		this.position = position;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(POSITION, this.position);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			this.position = savedInstanceState.getInt(POSITION);
		
		return null;
	}
}
