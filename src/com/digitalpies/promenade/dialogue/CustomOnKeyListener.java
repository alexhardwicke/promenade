package com.digitalpies.promenade.dialogue;

import android.content.DialogInterface;
import android.view.KeyEvent;

public class CustomOnKeyListener implements DialogInterface.OnKeyListener
{
	@Override
	public boolean onKey(DialogInterface receivedDialog, int keyCode, KeyEvent event)
	{
		// Used to prevent a search being started while the "Waiting for GPS" dialogue
		// is open. It checks both KEYCODE_SEARCH (search button) and KEYCODE_MENU, as
		// some phones, e.g. the SGS2, have no search button but simulate it with the
		// holding of the menu key.
		if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_MENU) return true;
		return false;
	}
}
