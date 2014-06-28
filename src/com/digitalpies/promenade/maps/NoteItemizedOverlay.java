package com.digitalpies.promenade.maps;

import com.digitalpies.promenade.R;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Note version of CustomItemizedOverlay.<br>
 * <br>
 * Overrides createBalloonOverlayView to show a custom note view.
 * 
 * @author Alex Hardwicke
 */
public class NoteItemizedOverlay extends CustomItemizedOverlay
{
	public static final String FIRST_NOTE_RUN = "FIRST_NOTE_RUN";

	public NoteItemizedOverlay(Drawable defaultMarker, CustomMapActivity activity, MapView mapView)
	{
		super(defaultMarker, activity, mapView);
	}

	@Override
	protected CustomOverlayView<OverlayItem> createBalloonOverlayView()
	{
		return new CustomNoteView(this.mapView.getContext(), getBalloonBottomOffset(), this.activity);
	}
	
	@Override
	protected void onBalloonOpen(int index)
	{
		super.onBalloonOpen(index);
		
		// Get the SharedPreferences and check if this is the first time the user is at this balloon. If so, inform
		// them with a Toast that they swipe to view other notes, and set the preferences so that this can't
		// happen again. Only runs if there is more than one note.
		if (this.activity.getNotes().size() > 0)
		{
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.activity);
			if (preferences.getBoolean(FIRST_NOTE_RUN, true))
			{
				Toast.makeText(this.activity, R.string.toast_can_swipe_notes, Toast.LENGTH_LONG).show();
				Editor editor = preferences.edit();
				editor.putBoolean(FIRST_NOTE_RUN, false);
				editor.apply();
			}
		}
	}
}
