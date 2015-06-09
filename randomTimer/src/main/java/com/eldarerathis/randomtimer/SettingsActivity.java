/*
 * This file is part of Random Timer.
 *
 * Random Timer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Random Timer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Random Timer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.eldarerathis.randomtimer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.MediaStore;
import android.webkit.WebView;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity 
implements OnPreferenceChangeListener, OnSharedPreferenceChangeListener, OnPreferenceClickListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int theme = (prefs.getString("pref_key_theme", "0").equals("0"))
				? R.style.LightSettingsTheme
						: R.style.DarkSettingsTheme;

		setTheme(theme);

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		RingtonePreference ringPref = (RingtonePreference)findPreference("pref_key_ringtone");
		DialogPreference minPref = (DialogPreference)findPreference("pref_key_min_time");
		DialogPreference maxPref = (DialogPreference)findPreference("pref_key_max_time");
		Preference licensePref = findPreference("pref_key_licenses");

		ringPref.setOnPreferenceChangeListener(this);
		minPref.setOnPreferenceChangeListener(this);
		maxPref.setOnPreferenceChangeListener(this);
		updateRingtoneSummary(ringPref, prefs.getString(ringPref.getKey(), null));
		updateTextSummaries();

		licensePref.setOnPreferenceClickListener(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	private void updateSummary(String key)
	{
		if (findPreference(key) instanceof DialogPreference && !key.equals("pref_key_theme"))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			DialogPreference p = (DialogPreference)findPreference(key);
			String defValue = key.equals("pref_key_max_time") ? "60" : "30";
			String summary = secondsToHMS(Integer.parseInt(prefs.getString(p.getKey(), defValue)));
			p.setSummary(summary);
		}
		else if (key.equals("pref_key_theme"))
		{
			ListPreference lp = (ListPreference)findPreference(key);
			int value = Integer.parseInt(lp.getValue());
			String[] entryValues = getResources().getStringArray(R.array.theme_names);
			lp.setSummary(entryValues[value]);
		}
		else if (key.equals("pref_key_about"))
		{
			Preference p = findPreference(key);
			try 
			{
				String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				p.setSummary("Version " + version);
			} 
			catch (NameNotFoundException e) 
			{
				// Shouldn't happen?
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) 
	{
		if (preference.getKey().equals("pref_key_licenses"))
		{
			WebView wv = new WebView(this);
			wv.loadUrl("file:///android_asset/licenses.html");

			new AlertDialog.Builder(this)
			.setTitle("Open Source Licenses")
			.setView(wv)
			.setNegativeButton("Close", new DialogInterface.OnClickListener() 
			{
				@Override
				public void onClick(DialogInterface dialog, int id) 
				{
					dialog.dismiss();
				}
			})
			.show();

			return false;
		}

		return true;
	}

	private void updateTextSummaries()
	{
		updateSummary("pref_key_min_time");
		updateSummary("pref_key_max_time");
		updateSummary("pref_key_theme");
		updateSummary("pref_key_about");
	}

	private void updateRingtoneSummary(RingtonePreference pref, String newValue)
	{
		String ringtoneName = "None";
		Uri uri = Uri.parse(newValue);

		if (uri.getScheme().equals("file"))
		{
			ringtoneName = uri.getLastPathSegment();
		}
		else if (uri.getScheme().equals("content"))
		{
			Cursor c = null;

			try
			{
				String[] projection = { MediaStore.Audio.Media.TITLE  };
				c = this.getContentResolver().query(uri, projection, null, null, null);

				if (c != null && c.getCount() > 0)
				{
					int columnIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
					c.moveToFirst();
					ringtoneName = c.getString(columnIndex);
				}
			}
			catch (SQLiteException ex)
			{
				Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(newValue));

				if (ringtone != null)
					ringtoneName = ringtone.getTitle(this);
			}
			finally
			{
				if (c != null)
					c.close();
			}
		}

		pref.setSummary(ringtoneName);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) 
	{
		updateSummary(key);

		if (key.equals("pref_key_theme"))
			recreateCompat();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) 
	{
		if (preference instanceof DialogPreference)
		{
			if (preference.getKey().equals("pref_key_min_time"))
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				int maxValue = Integer.parseInt(prefs.getString("pref_key_max_time", "60"));

				if (maxValue < Integer.parseInt(newValue.toString()))
				{
					showInvalidAlert("Invalid Minimum", "Minimum time cannot be greater than maximum time.");
					return false;
				}
				else if (Integer.parseInt(newValue.toString()) < 1)
				{
					showInvalidAlert("Invalid Value", "Time cannot be less than 1 second.");
					return false;
				}
			}
			else if (preference.getKey().equals("pref_key_max_time"))
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				int minValue = Integer.parseInt(prefs.getString("pref_key_min_time", "30"));

				if (minValue > Integer.parseInt(newValue.toString()))
				{
					showInvalidAlert("Invalid Maximum", "Maximum time cannot be less than maximum time.");
					return false;
				}
				else if (Integer.parseInt(newValue.toString()) < 1)
				{
					showInvalidAlert("Invalid Value", "Time cannot be less than 1 second");
					return false;
				}
			}

			updateSummary(preference.getKey());
		}
		else if (preference instanceof RingtonePreference)
		{
			updateRingtoneSummary((RingtonePreference)preference, (String)newValue);
		}

		return true;
	}

	@TargetApi(8)
	private void recreateCompat()
	{
		if (android.os.Build.VERSION.SDK_INT >= 11)
		{
			recreate();
		}
		else
		{
			startActivity(getIntent());
			finish();
		}
	}

	private String secondsToHMS(int totalSeconds)
	{
		String output = "";
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds %60;

		if (hours > 0)
			output += String.valueOf(hours) + (hours > 1 ? " hours" : " hour");

		if (minutes > 0)
		{
			if (output.length() > 0)
				output += ", ";

			output += String.valueOf(minutes) + (minutes > 1 ? " minutes" : " minute");
		}

		if (seconds > 0 || output.length() == 0)
		{
			if (output.length() > 0)
				output += ", ";

			output += String.valueOf(seconds) + (seconds > 1 || seconds == 0 ? " seconds" : " second");
		}

		return output;
	}

	private void showInvalidAlert(String title, String message)
	{
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message)
		.setPositiveButton("Okay", new DialogInterface.OnClickListener() 
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1) 
			{
				arg0.dismiss();
			}
		})
		.show();
	}
}
