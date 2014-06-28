package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A custom dialogue that shows a list of all available tags as checkboxes to allow the user to
 * filter the walk list.<br>
 * <br>
 * If the user presses ok, the selected items are sent to WalkListActivity where they are used
 * to filter the walk list.
 * 
 * @author Alex Hardwicke
 */
public class TagListDialogue extends DialogFragment
{
	private String[] tagsString;
	protected ArrayList<Tag> tags;
	protected boolean[] checkedItems;

	public final static String TAGS_STRING = "TAGS_STRING";
	public final static String TAGS = "TAGS";
	public final static String CHECKED_ITEMS = "CHECKED_ITEMS";
	
	public static TagListDialogue newInstance(String[] tagsString, ArrayList<Tag> tags, boolean[] checkedItems)
	{
		return new TagListDialogue(tagsString, tags, checkedItems);
	}
	
	public TagListDialogue()
	{
		super();
	}

	private TagListDialogue(String[] tagsString, ArrayList<Tag> tags, boolean[] checkedItems)
	{
		super();
		this.tagsString = tagsString;
		this.tags = tags;
		this.checkedItems = checkedItems;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putStringArray(TAGS_STRING, this.tagsString);
		outState.putParcelableArrayList(TAGS, this.tags);
		outState.putBooleanArray(CHECKED_ITEMS, this.checkedItems);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
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
						TagListDialogue.this.checkedItems[which] = isChecked;
					}
				}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						// Create an ArrayList, and add tags to it if they are checked, by using
						// the checkedItems boolean array
						ArrayList<Tag> checkedTags = new ArrayList<Tag>();
						
						for (int i = 0; i < TagListDialogue.this.checkedItems.length; i++)
						{
							if (TagListDialogue.this.checkedItems[i]) checkedTags.add(TagListDialogue.this.tags.get(i));
						}
						
						((WalkListActivity) getActivity()).filterWalksByTag(checkedTags);
					}
				}).setNegativeButton(android.R.string.cancel, null).create();
		
		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_tags);
		String title = getString(R.string.tags);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
