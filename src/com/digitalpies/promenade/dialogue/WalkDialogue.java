package com.digitalpies.promenade.dialogue;

import java.util.ArrayList;
import java.util.Collections;

import com.digitalpies.promenade.R;
import com.digitalpies.promenade.database.DataSource;
import com.digitalpies.promenade.database.Tag;
import com.digitalpies.promenade.database.Walk;
import com.digitalpies.promenade.maps.CustomMapActivity;
import com.digitalpies.promenade.walklist.CustomListActivity;
import com.digitalpies.promenade.walklist.WalkListActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * A custom dialogue that lets the user create, edit or save a walk.<br>
 * <br>
 * Takes in a String stating the task required (new walk, save walk or edit walk).<br>
 * Inflates a custom layout, keeping references to the edittexts to be used to retrieve
 * data when saving the instance state.<br>
 * <br>
 * Repopulates the dialogue if the instance state isn't null (i.e orientation change).<br>
 * <br>
 * Implements various listeners to handle clicks on most of the buttons.<br>
 * <br>
 * Lastly, contains many methods to handle the generation of tags and checkboxes when the
 * dialogue is first created, re-created, and when new tags are added.
 * 
 * @author Alex Hardwicke
 */
public class WalkDialogue extends DialogFragment
{
	protected static final String WALK = "WALK";
	protected static final String NAME = "NAME";
	protected static final String TASK = "TASK";
	protected static final String TAGS = "TAGS";
	protected static final String DESCRIPTION = "DESCRIPTION";
	protected static final String TAG_LIST = "TAG_LIST";
	protected static final String ALL_TAGS = "ALL_TAGS";
	protected static final String FIRST_WALK_RUN = "FIRST_WALK_RUN";

	protected ArrayList<Tag> checkedTagsList, allTags;
	protected LinearLayout tagLayout;
	protected EditText nameEditText, descriptionEditText, tagEditText;
	protected Walk selectedWalk;
	protected Drawable icon;

	protected String name, description, tags, task, title;

	/**
	 * Used to generate a new dialogue with no walk provided. Called by WalkListActivity when starting a new walk.
	 * 
	 * @param task	The Task to be performed (Start)
	 * 
	 * @return		The newly created fragment
	 */
	public static WalkDialogue newInstance(String task)
	{
		return new WalkDialogue(task);
	}

	/**
	 * Used to generate a new dialogue with a walk provided. Called by WalkListActivity, MapViewActivity and
	 * MapWalkActivity when editing or saving a walk.
	 * 
	 * @param task			The Task to be performed (Save vs Edit)
	 * @param selectedWalk	The walk to be edited or saved.
	 * 
	 * @return				The newly created fragment.
	 */
	public static WalkDialogue newInstance(String task, Walk selectedWalk)
	{
		return new WalkDialogue(task, selectedWalk);
	}

	public WalkDialogue()
	{
		super();
	}

	public WalkDialogue(String task)
	{
		this();
		this.task = task;
	}

	public WalkDialogue(String task, Walk selectedWalk)
	{
		this(task);
		this.selectedWalk = selectedWalk;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Set the name, description and tags and store them in the Bundle, plus the Walk.
		this.name = this.nameEditText.getText().toString();
		this.description = this.descriptionEditText.getText().toString();
		this.tags = this.tagEditText.getText().toString();
		outState.putParcelable(WALK, this.selectedWalk);
		outState.putString(NAME, this.name);
		outState.putString(DESCRIPTION, this.description);
		outState.putString(TAGS, this.tags);
		outState.putString(TASK, this.task);
		outState.putParcelableArrayList(TAG_LIST, this.checkedTagsList);
		outState.putParcelableArrayList(ALL_TAGS, this.allTags);
	}

	/**
	 * Creates the walk dialogue.<br>
	 * <br>
	 * Sets up the appropriate variables:<br>
	 * <b>*</b> If savedInstanceState isn't null, restores the values saved before the re-creation
	 *   		started, and sets the newInstance boolean to false.<br>
	 * <b>*</b> Otherwise, if the provided walk isn't null, it retrieves the name and description
	 * 			from the walk, and makes a copy of the walk's tags. If the provided walk is null,
	 * 			it initialises the checkedtags list as an empty ArrayList. In either case, it sets
	 * 			up the allTags ArrayList to contain all tags in the database, and sets the newInstance
	 * 			boolean to true.<br>
	 * <br>
	 * It then sets up the icon and title of the dialogue depending upon the task required.<br>
	 * <br>
	 * Next, it inflates the layout, sets up the EditTexts as appropriate and gets a reference to the
	 * LinearLayout that will contain the tag checkboxes. Next, it creates the dialog itself and
	 * either initialises or updates the tags, depending on the status of the newInstance boolean.<br>
	 * <br>
	 * After that, it sets up an OnShowListener and OnKeyPressedListener for the dialogue, to prevent
	 * the dialogue automatically being dismissed when buttons are pressed, and to prevent search
	 * requests dismissing the dialogue.<br>
	 * <br>
	 * It then sets up a TextChangedListener on the tag EditText to prevent upper case characters
	 * being entered, and sets up an onClickListener on the new tag button which adds the entered
	 * tag(s) into the checkbox layout when pressed, clears the EditText and dismisses the keyboard.
	 * <br>
	 * Finally, it sets up the custom dialogue title for the dialogue and returns it.
	 * 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		boolean newInstance;

		// If there's savedInstanceState data (i.e. the dialogue is being re-created)
		if (savedInstanceState != null)
		{
			this.selectedWalk = (Walk) savedInstanceState.getParcelable(WALK);
			this.name = savedInstanceState.getString(NAME);
			this.description = savedInstanceState.getString(DESCRIPTION);
			this.tags = savedInstanceState.getString(TAGS);
			this.checkedTagsList = savedInstanceState.getParcelableArrayList(TAG_LIST);
			this.allTags = savedInstanceState.getParcelableArrayList(ALL_TAGS);
			this.task = savedInstanceState.getString(TASK);
			newInstance = false;
		}
		else
		{
			// If the walk isn't null, get the details so the dialogue can be populated
			if (this.selectedWalk != null)
			{
				this.name = this.selectedWalk.getName();
				this.description = this.selectedWalk.getDescription();
				// Getting copy of the tags rather than a direct reference so that they don't
				// get changed until I want them to
				this.checkedTagsList = new ArrayList<Tag>(this.selectedWalk.getTags());
			}
			// Walk is null, so checkedTagsList should start empty
			else
				this.checkedTagsList = new ArrayList<Tag>();

			// The allTags ArrayList should contain all tags from the DataSource
			this.allTags = DataSource.getAllTags();
			newInstance = true;
		}

		// Set up the icon and title
		if (this.task == CustomListActivity.NEW_WALK_TASK)
		{
			this.icon = getResources().getDrawable(R.drawable.ic_menu_new_walk);
			this.title = getString(R.string.start_walk);
		}
		else if (this.task == CustomMapActivity.SAVE_WALK_TASK)
		{
			this.title = getString(R.string.save_walk);
			this.icon = getResources().getDrawable(R.drawable.ic_menu_accept);
		}
		else if (this.task == CustomListActivity.EDIT_WALK_TASK)
		{
			this.title = getString(R.string.edit_walk);
			this.icon = getResources().getDrawable(R.drawable.ic_menu_edit);
		}

		// Get and inflate the layout, and then the three EditTexts and LinearLayout
		final Activity activity = getActivity();
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.dialogue_walk, null);
		ImageButton tagButton = (ImageButton) view.findViewById(R.id.new_tag_button);

		this.tagLayout = (LinearLayout) view.findViewById(R.id.tags_layout);

		this.nameEditText = (EditText) view.findViewById(R.id.walk_name);
		this.descriptionEditText = (EditText) view.findViewById(R.id.walk_description);
		this.tagEditText = (EditText) view.findViewById(R.id.walk_tags);
		
		// Adding in the data to the editTexts
		this.nameEditText.setText(this.name);
		this.descriptionEditText.setText(this.description);
		this.tagEditText.setText(this.tags);

		// Move the cursor to the end of the nameEditText
		this.nameEditText.setSelection(this.nameEditText.length());

		// Create the dialogue
		AlertDialog alertDialog = new AlertDialog.Builder(activity).setView(view).setIcon(this.icon)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.cancel();
					}
				}).create();

		// Initialise the tags if it's a new instance, otherwise update them
		if (newInstance)
			initialiseTags(this.selectedWalk);
		else
			updateTags(this.checkedTagsList);

		// Set up the listeners
		alertDialog.setOnShowListener(new WalkOnShowListener(alertDialog, activity, this.task, this.selectedWalk,
				this.checkedTagsList));
		alertDialog.setOnKeyListener(new CustomOnKeyListener());

		this.tagEditText.addTextChangedListener(new TagTextWatcher(this.tagEditText));

		tagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				addTags(WalkDialogue.newTags(WalkDialogue.this.tagEditText));
				
				// Clear the EditText and dismiss the keyboard
				WalkDialogue.this.tagEditText.setText("");
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(WalkDialogue.this.tagEditText.getWindowToken(), 0);
			}
		});
		
		tagButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v)
			{
				Toast.makeText(activity, R.string.add_tag, Toast.LENGTH_SHORT).show();
				return false;
			}
		});

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		// Check if this is the first time the user has opened the walk dialogue. If so, inform them with
		// a Toast that they can divide multiple tags with commas, and set the preferences so that this
		// isn't shown again
		if (preferences.getBoolean(FIRST_WALK_RUN, true))
		{
			Toast.makeText(activity, R.string.toast_divide_tags_with_commas, Toast.LENGTH_LONG).show();
			Editor editor = preferences.edit();
			editor.putBoolean(FIRST_WALK_RUN, false);
			editor.apply();
		}
		// Return the dialogue with a custom title
		return CustomTitleBuilder.setCustomTitle(activity, this.title, this.icon, alertDialog);
	}

	protected static ArrayList<Tag> newTags(EditText editText)
	{
		// Get the list of tags and split them up using the dividing commas
		String tagList = editText.getText().toString();
		String[] tagArray = tagList.split(",");
		// Add each tag to an ArrayList and send it to the activity
		ArrayList<Tag> newTags = new ArrayList<Tag>();
		for (String string : tagArray)
		{
			// Remove spaces from the start and end of the String
			String trimmedString = string.trim();
			if (trimmedString.length() != 0) newTags.add(new Tag(trimmedString));
		}
		return newTags;
	}

	/**
	 * Cancels the dialogue. Only does something different if it's the "NEW WALK" dialogue, in
	 * which case it stops the GPS service.
	 * 
	 */
	@Override
	public void onCancel(DialogInterface dialog)
	{
		if (this.task == CustomListActivity.NEW_WALK_TASK)
		{
			((WalkListActivity) getActivity()).cancelGPS();
		}
		super.onCancel(dialog);
	}

	/**
	 * Used when the dialogue is first opened - populates the LinearLayout with all tags
	 * that exist in the database, and checks if the walk contains them, checking them
	 * if so.
	 * 
	 * @param walk	The walk for the dialogue.
	 */
	public void initialiseTags(Walk walk)
	{
		// Get the walk's tags, assuming that the walk isn't null
		ArrayList<Tag> walkTags = null;
		if (walk != null) walkTags = walk.getTags();
		for (Tag tag : WalkDialogue.this.allTags)
		{
			// Create a checkbox and set the text to the tag
			CheckBox checkBox = new CheckBox(getActivity());
			checkBox.setText(tag.getName());

			// If the walk's tags ArrayList isn't null and contains the tag, check the checkbox
			if (walkTags != null) if (walkTags.contains(tag)) checkBox.setChecked(true);

			// Provide a listener that keeps an up to date record of whether it's checked or not
			CustomOnCheckChangeListener listener = new CustomOnCheckChangeListener();
			checkBox.setOnCheckedChangeListener(listener);

			// Add the checkbox to the tag layout
			WalkDialogue.this.tagLayout.addView(checkBox);
		}
	}

	/**
	 * Used when the dialogue is re-created (e.g. rotated) - populates the LinearLayout with all tags
	 * that exist in the database, and if the provided list contains the tag, checks it.
	 * 
	 * @param walk
	 */
	public void updateTags(ArrayList<Tag> checkedTags)
	{
		// Remove all current checkboxes
		WalkDialogue.this.tagLayout.removeAllViews();
		for (Tag tag : WalkDialogue.this.allTags)
		{
			// Create a checkbox and set the text to the tag
			CheckBox checkBox = new CheckBox(getActivity());
			checkBox.setText(tag.getName());

			// If the checked tags ArrayList contains the tag, check the checkbox
			if (checkedTags.contains(tag)) checkBox.setChecked(true);

			// Provide a listener that keeps an up to date record of whether it's checked or not
			CustomOnCheckChangeListener listener = new CustomOnCheckChangeListener();
			checkBox.setOnCheckedChangeListener(listener);

			// Add the checkbox to the tag layout
			WalkDialogue.this.tagLayout.addView(checkBox);
		}
	}

	/**
	 * Adds the provided Tags to the dialogue, all set to be checked. If a supplied
	 * tag already exists in either ArrayList, it isn't added to that one - this
	 * generally means that if a user types a tag that already exists, it just gets
	 * checked.
	 * 
	 * @param newTags	The tags to be added.
	 */
	public void addTags(ArrayList<Tag> newTags)
	{
		for (Tag tag : newTags)
		{
			if (!this.checkedTagsList.contains(tag)) this.checkedTagsList.add(tag);
			if (!this.allTags.contains(tag)) this.allTags.add(tag);
		}
		Collections.sort(this.allTags);
		updateTags(this.checkedTagsList);
	}

	/**
	 * Used to keep an up to date list of checked tags.
	 * 
	 * @author Alex Hardwicke
	 */
	private class CustomOnCheckChangeListener implements OnCheckedChangeListener
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			Tag tag = new Tag(buttonView.getText().toString());
			if (isChecked && !WalkDialogue.this.checkedTagsList.contains(tag))
				WalkDialogue.this.checkedTagsList.add(tag);
			else
				WalkDialogue.this.checkedTagsList.remove(tag);
		}
	}
}
