package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.walklist.CustomListActivity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;

/**
 * Shows a progress dialogue that shows the progress as walks are being deleted.<br>
 * <br>
 * The dialogue's progress is updated by the AsyncTask using the dialogue.
 * 
 * @author Alex Hardwicke
 */
public class DeletingWalksDialogue extends DialogFragment
{
	private int size;
	private int progress = 0;
	private ProgressDialog dialog;
	
	public static String SIZE = "SIZE";
	public static String PROGRESS = "PROGRESS";
	
	public static DeletingWalksDialogue newInstance(int size)
	{
		return new DeletingWalksDialogue(size);
	}
	
	public DeletingWalksDialogue()
	{
		super();
	}

	private DeletingWalksDialogue(int size)
	{
		super();
		this.size = size;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(SIZE, this.size);
		outState.putInt(PROGRESS, this.dialog.getProgress());
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Create and save the progress dialogue (to allow saving of progress on rotate)
		ProgressDialog createdDialog = new ProgressDialog(getActivity());
		this.dialog = createdDialog;
		
		if (savedInstanceState != null)
		{
			this.size = savedInstanceState.getInt(SIZE);
			this.progress = savedInstanceState.getInt(PROGRESS);
		}
		
		// Setting horizontal style, max value and progress on creation
		createdDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		createdDialog.setMax(this.size);
		createdDialog.setProgress(this.progress);
		
		// Set up the frame and button
		createdDialog.setMessage(getString(R.string.deleting_walks_message));
		createdDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface receivedDialog, int buttonId)
			{
				receivedDialog.cancel();
			}});
		
		// Hijacking the menu and search buttons
		createdDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface receivedDialog, int keyCode, KeyEvent event)
			{
				// Used to prevent a search being started while the "Deleting" dialogue
				// is open. It checks both KEYCODE_SEARCH (search button) and KEYCODE_MENU, as
				// some phones, e.g. the SGS2, have no search button but simulate it with the
				// holding of the menu key.
				if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU)
					return true;
				return false;
			}
		});
		
		Drawable icon = getResources().getDrawable(R.drawable.ic_menu_discard);
		String title = getString(R.string.deleting_walks);

		return CustomTitleBuilder.setCustomTitle(getActivity(), title, icon, createdDialog);
	}
	
	/**
	 * Intercept when the dialogue is cancelled and cancel the deletion AsyncTask
	 */
	@Override
	public void onCancel(DialogInterface receivedDialog)
	{
		((CustomListActivity) getActivity()).cancelDeletion();
		super.onCancel(receivedDialog);
	}
}
