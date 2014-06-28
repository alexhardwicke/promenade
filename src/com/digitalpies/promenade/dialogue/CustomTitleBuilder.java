package com.digitalpies.promenade.dialogue;

import com.digitalpies.promenade.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

public class CustomTitleBuilder
{
	public static Dialog setCustomTitle(Context context, String title, Drawable icon, AlertDialog dialog)
	{
		// Setting up a custom title layout. Create a TextView, set the background to the stripes
		// drawable, which has been set to tile REPEAT both X and Y, set the text of the TextView to
		// the text provided and set the text size and colour, set the left compound drawable of the
		// TextView to the icon provided, add padding, set the dialogue's title to the created TextView
		// and return the dialogue.
		TextView titleView = new TextView(context);
		BitmapDrawable bitmap = (BitmapDrawable) context.getResources().getDrawable(R.drawable.stripes);
		bitmap.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
		icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
		titleView.setBackgroundDrawable(bitmap);
		titleView.setText(title);
		titleView.setTextColor(Color.WHITE);
		titleView.setTextSize(22);
		titleView.setCompoundDrawables(icon, null, null, null);
		titleView.setCompoundDrawablePadding(16);
		titleView.setPadding(24, 24, 24, 24);
		dialog.setCustomTitle(titleView);

		return dialog;
	}
}
