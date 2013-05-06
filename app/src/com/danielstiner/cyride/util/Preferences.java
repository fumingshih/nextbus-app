package com.danielstiner.cyride.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

	public static final String PREFERENCE_AGENCY = "agency";

	public static String getAgency(Context c) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(c);
		return sharedPref.getString(PREFERENCE_AGENCY, Constants.AGENCY);
	}

}
