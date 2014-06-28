package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.walklist.CustomListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A custom dialogue that shows a list of all available tags as checkboxes to allow the user to
 * remove tags from walks.<br>
 * <br>
 * If the user presses ok, the selected items are sent to a DeleteTagDialogue.
 * 
 * @author Alex Hardwicke
 */
public class TagRemoveDialogue extends DialogFragment
{
	private ArrayList<Walk> walks;
	private String[] tagsString;
	protected ArrayList<Tag> tags;
	protected boolean[] checkedItems;

	public final static String WALKS = "WALKS";
	public final static String TAGS_STRING = "TAGS_STRING";
	public final static String TAGS = "TAGS";
	public final static String CHECKED_ITEMS = "CHECKED_ITEMS";

	public static TagRemoveDialogue newInstance(ArrayList<Walk> walks, String[] tagsString, ArrayList<Tag> tags)
	{
		return new TagRemoveDialogue(walks, tagsString, tags);
	}

	public TagRemoveDialogue()
	{
		super();
	}

	private TagRemoveDialogue(ArrayList<Walk> walks, String[] tagsString, ArrayList<Tag> tags)
	{
		super();
		this.walks = walks;
		this.tagsString = tagsString;
		this.tags = tags;
		this.checkedItems = new boolean[tags.size()];
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelableArrayList(WALKS, this.walks);
		outState.putStringArray(TAGS_STRING, this.tagsString);
		outState.putParcelableArrayList(TAGS, this.tags);
		outState.putBooleanArray(CHECKED_ITEMS, this.checkedItems);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			this.walks = savedInstanceState.getParcelableArrayList(WALKS);
			this.tagsString = savedInstanceState.getStringArray(TAGS_STRING);
			this.tags = savedInstanceState.getParcelableArrayList(TAGS);
			this.checkedItems = savedInstanceState.getBooleanArray(CHECKED_ITEMS);
		}

		AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
				// Create the checkboxes, values = tagsString, ticked or not = checkedItems
				.setMultiChoiceItems(this.tagsString, this.checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked)
					{
						// Keep the array up-to-date
						TagRemoveDialogue.this.checkedItems[which] = isChecked;
					}
				}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						// Create an ArrayList, and add tags to it if they are checked, by using
						// the checkedItems boolean array
						ArrayList<Tag> checkedTags = new ArrayList<Tag>();

						for (int i = 0; i < TagRemoveDialogue.this.checkedItems.length; i++)
						{
							if (TagRemoveDialogue.this.checkedItems[i]) checkedTags.add(TagRemoveDialogue.this.tags.get(i));
						}

						// Show a confirm-deletion dialogue
						// Only show the dialogue if one doesn't already exist (can happen if user taps quickly on the button)
						FragmentManager manager = getFragmentManager();
						if (manager.findFragmentByTag(CustomListActivity.DIALOGUE_FRAGMENT_DELETE_TAGS) == null)
						{
							DialogFragment newFragment = DeleteTagDialogue.newInstance(checkedTags);
							newFragment.show(manager, CustomListActivity.DIALOGUE_FRAGMENT_DELETE_TAGS);
						}
					}
				}).setNegativeButton(android.R.string.cancel, null).create();
		
		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_tags);
		String title = getString(R.string.tags_remove);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
