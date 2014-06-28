package com.digitalpies.promenade.walklist;

import android.content.SearchRecentSuggestionsProvider;

/**
 * A very simple SearchSuggestionProvider.<br>
 * <br>
 * Has to be a separate public class to be used in the AndroidManifest file.
 * 
 * @author Alex Hardwicke
 */
public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider
{
	public final static String AUTHORITY = "com.digitalpies.promenade.walklist.SearchSuggestionProvider";
	public final static int MODE = DATABASE_MODE_QUERIES;
	
	public SearchSuggestionProvider()
	{
		setupSuggestions(AUTHORITY, MODE);
	}
}
