package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.walklist.CustomListActivity;

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
 * Shows a dialogue asking the user if they are sure they wish to delete the selected walk(s).<br>
 * <br>
 * If they confirm that they want to, deleteWalk is called in the originating activity.
 * 
 * @author Alex Hardwicke
 */
public class DeleteWalkDialogue extends DialogFragment
{
	protected ArrayList<Walk> selectedWalks;
	protected boolean selected;
	protected boolean photos;

	public final static String PHOTOS = "PHOTOS";
	public final static String SELECTED = "SELECTED";
	public final static String WALK = "WALK";

	public static DeleteWalkDialogue newInstance(ArrayList<Walk> selectedWalks, boolean photos)
	{
		return new DeleteWalkDialogue(selectedWalks, photos);
	}

	public DeleteWalkDialogue()
	{
		super();
	}

	private DeleteWalkDialogue(ArrayList<Walk> selectedWalks, boolean photos)
	{
		super();
		this.selectedWalks = selectedWalks;
		this.photos = photos;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelableArrayList(WALK, this.selectedWalks);
		outState.putBoolean(SELECTED, this.selected);
		outState.putBoolean(PHOTOS, this.photos);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			this.selectedWalks = savedInstanceState.getParcelableArrayList(WALK);
			this.selected = savedInstanceState.getBoolean(SELECTED);
			this.photos = savedInstanceState.getBoolean(PHOTOS);
		}

		// Get and inflate the layout, and then the three TextViews
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		View view = inflater.inflate(R.layout.dialogue_delete_walk, null);

		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.delete_photos);

		if (this.photos)
		{
			checkBox.setChecked(this.selected);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1)
				{
					DeleteWalkDialogue.this.selected = checkBox.isChecked();
				}

			});
		}
		else
			view.findViewById(R.id.delete_photos).setVisibility(View.GONE);
		String title;
		if (this.selectedWalks.size() == 1)
			title = getString(R.string.delete_walk_question);
		else
			title = getString(R.string.delete_walks_question);
		// Create and return the dialogue.
		AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(view)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int buttonId)
					{
						((CustomListActivity) getActivity()).deleteWalk(DeleteWalkDialogue.this.selectedWalks,
								checkBox.isChecked());
					}
				}).setNegativeButton(android.R.string.no, null).create();

		Drawable icon = getResources().getDrawable(R.drawable.ic_alert_warning);
		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, alertDialog);
	}
}
