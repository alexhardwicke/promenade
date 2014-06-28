package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.walklist.CustomListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A custom dialogue asking the user if they are sure they wish to delete tags.<br>
 * <br>
 * If they confirm that they are, it removes the tags from the database and then updates the activity's list.
 * 
 * @author Alex Hardwicke
 */
public class DeleteTagDialogue extends DialogFragment
{
	protected ArrayList<Tag> checkedTags;
	public final static String TAGS = "TAGS";

	public static DeleteTagDialogue newInstance(ArrayList<Tag> checkedTags)
	{
		return new DeleteTagDialogue(checkedTags);
	}

	public DeleteTagDialogue()
	{
		super();
	}

	private DeleteTagDialogue(ArrayList<Tag> checkedTags)
	{
		super();
		this.checkedTags = checkedTags;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelableArrayList(TAGS, this.checkedTags);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null) this.checkedTags = savedInstanceState.getParcelableArrayList(TAGS);

		// Set the title's text to singular or plural as appropriate
		String title;
		if (this.checkedTags.size() == 1)
			title = getString(R.string.delete_tag);
		else
			title = getString(R.string.delete_tags);

		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.cannot_be_undone)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						// Remove the tags from the database and update the activity's list.
						DataSource.deleteTags(DeleteTagDialogue.this.checkedTags);
						((CustomListActivity) getActivity()).updateWalksList();
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
