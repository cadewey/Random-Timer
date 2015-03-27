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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class TimeEntryDialog extends DialogPreference 
{
	private Context _ctxt = null;
	private String _dialogMessage = null;
	private String _prefKey = null;
	private String _prefDefault = null;

	public TimeEntryDialog(Context context, AttributeSet attrs) 
	{
		super(context, attrs);

		_ctxt = context;
		setPersistent(false);
		setDialogLayoutResource(R.layout.time_entry_dialog);

		for (int i = 0; i < attrs.getAttributeCount(); ++i)
		{
			if (attrs.getAttributeName(i).equals("dialogMessage"))
				_dialogMessage = context.getResources().getString(attrs.getAttributeResourceValue(i, 0));
			else if (attrs.getAttributeName(i).equals("key"))
			{
				_prefKey = attrs.getAttributeValue(i);
				_prefDefault = _prefKey.equals("pref_key_max_time") ? "60" : "30";
			}
		}
	}

	@Override
	protected void onBindDialogView(View view)
	{
		super.onBindDialogView(view);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_ctxt);
		TextView msg = (TextView)view.findViewById(R.id.time_entry_dialog_message);
		TextView txtHours = (TextView)view.findViewById(R.id.time_dialog_hours);
		TextView txtMinutes = (TextView)view.findViewById(R.id.time_dialog_minutes);
		TextView txtSeconds = (TextView)view.findViewById(R.id.time_dialog_seconds);

		int currSeconds = Integer.parseInt(prefs.getString(_prefKey, _prefDefault));
		int hours = currSeconds / 3600;
		int minutes = (currSeconds % 3600) / 60;
		int seconds = (currSeconds % 60);

		msg.setText(_dialogMessage);
		txtHours.setText(String.valueOf(hours));
		txtMinutes.setText(String.valueOf(minutes));
		txtSeconds.setText(String.valueOf(seconds));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (which == DialogInterface.BUTTON_POSITIVE)
		{
			try
			{
				TextView txtHours = (TextView)((AlertDialog)dialog).findViewById(R.id.time_dialog_hours);
				TextView txtMinutes = (TextView)((AlertDialog)dialog).findViewById(R.id.time_dialog_minutes);
				TextView txtSeconds = (TextView)((AlertDialog)dialog).findViewById(R.id.time_dialog_seconds);

				String hStr = txtHours.getText().toString();
				String mStr = txtMinutes.getText().toString();
				String sStr = txtSeconds.getText().toString();

				int h = isNullOrEmpty(hStr) ? 0 : Integer.parseInt(hStr) * 3600;
				int m = isNullOrEmpty(mStr) ? 0 : Integer.parseInt(mStr) * 60;
				int s = isNullOrEmpty(sStr) ? 0 : Integer.parseInt(sStr);
				int totalSeconds = h + m + s;

				if (callChangeListener(totalSeconds))
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_ctxt);
					Editor e = prefs.edit();
					e.putString(_prefKey, String.valueOf(totalSeconds));
					e.commit();
				}
			}
			catch (NumberFormatException ex)
			{
				new AlertDialog.Builder(_ctxt)
				.setTitle("Invalid Value")
				.setMessage("The time entered cannot be greater than " + String.valueOf(Integer.MAX_VALUE + " seconds."))
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
	}

	private boolean isNullOrEmpty(String s)
	{
		return s == null || s.length() == 0;
	}
}
