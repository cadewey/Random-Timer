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

import java.util.Random;

import com.beardedhen.androidbootstrap.BootstrapButton;

import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity 
{
	private int _currTheme = -1;
	private boolean _running = false;

	private static CountDownTimer _timer;
	private static Random _rand = new Random();
	private static Ringtone _sfx;
	private static Vibrator _vibrator;
	private SharedPreferences _prefs;

	private String _startText;
	private String _stopText;
	private String _stoppedText;
	private String _runningText;
	private String _timeUpText;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		checkTheme();
		setResult(42);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		_startText = getResources().getString(R.string.start_text);
		_stopText = getResources().getString(R.string.stop_text);
		_stoppedText = getResources().getString(R.string.stopped_text);
		_runningText = getResources().getString(R.string.running_text);
		_timeUpText = getResources().getString(R.string.time_up_text);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/* HACK: Some LGE devices ship with a broken version of the support library.
	 * In these cases, pressing the menu button causes an NPE. The next two
	 * overrides attempt to workaround this issue.
	 * 
	 * AOSP buck report: https://code.google.com/p/android/issues/detail?id=78154
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16) &&
				(Build.MANUFACTURER.compareTo("LGE") == 0)) 
		{
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{
		if ((keyCode == KeyEvent.KEYCODE_MENU) && (Build.VERSION.SDK_INT <= 16) &&
				(Build.MANUFACTURER.compareTo("LGE") == 0))
		{
			openOptionsMenu();
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		checkTheme();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (_timer != null)
			_timer.cancel();
	}

	@Override
	public void onBackPressed()
	{
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getTitle().toString().equals("Settings"))
		{
			startActivityForResult(new Intent("com.eldarerathis.randomtimer.intent.action.OPEN_SETTINGS"), 0);
		}

		return false;
	}

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

	private void checkTheme()
	{
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int theme = (_prefs.getString("pref_key_theme", "0").equals("0"))
				? R.style.AppThemeLight
						: R.style.AppThemeDark;

		setTheme(_currTheme);

		if (theme != _currTheme)
		{
			boolean needToRecreate = _currTheme != -1;

			_currTheme = theme;
			setTheme(_currTheme);

			if (needToRecreate)
				recreateCompat();
		}
	}

	public void btnStartClick(View view)
	{
		if (!_running)
		{
			startTimer(view);
		}
		else
		{
			_running = false;
			final BootstrapButton startButton = (BootstrapButton)view;
			final TextView textRunning = (TextView)findViewById(R.id.txtRunning);
			final ImageView imgView = (ImageView)findViewById(R.id.imgRunning);

			_timer.cancel();
			_timer = null;

			startButton.setText(_startText);
			startButton.setBootstrapType("success");

			Drawable yellowRing = getResources().getDrawable(R.drawable.yellow_ring);
			imgView.setImageDrawable(yellowRing);
			imgView.clearAnimation();

			textRunning.setText(_stoppedText);
		}
	}

	private void startTimer(View view)
	{
		_running = true;
		final BootstrapButton startButton = (BootstrapButton)view;
		final TextView textRunning = (TextView)findViewById(R.id.txtRunning);
		final ImageView imgView = (ImageView)findViewById(R.id.imgRunning);

		startButton.setText(_stopText);
		startButton.setBootstrapType("danger");

		Drawable greenRing = getResources().getDrawable(R.drawable.green_ring);
		imgView.setImageDrawable(greenRing);

		textRunning.setText(_runningText);
		imgView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.infinite_rotation));

		int minTime = Integer.parseInt(_prefs.getString("pref_key_min_time", "30"));
		int maxTime = Integer.parseInt(_prefs.getString("pref_key_max_time", "60"));
		int time = _rand.nextInt((maxTime - minTime) + 1) + minTime;

		_timer = new CountDownTimer(time * 1000 /* milliseconds */, Integer.MAX_VALUE)
		{
			@Override
			public void onFinish() 
			{
				boolean repeat = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_key_repeat", false);
				Drawable redRing = getResources().getDrawable(R.drawable.red_ring);
				imgView.setImageDrawable(redRing);
				textRunning.setText(_timeUpText);

				if (!repeat)
				{
					_running = false;
					_timer = null;
					startButton.setText(_startText);
					startButton.setBootstrapType("success");
				}

				if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_key_sound", false))
					playNotificationSound();

				if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("pref_key_vibrate",  false))
					vibrate();

				if (repeat)
				{
					new Thread()
					{
						@Override
						public void run()
						{
							do
							{
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							while (_sfx != null && _sfx.isPlaying());

							// Will be null if "Stop" was pressed while we were waiting to restart
							if (_timer != null)
							{
								runOnUiThread(new Runnable() 
								{
									public void run() 
									{
										startTimer(findViewById(R.id.btnStart));
									}
								});
							}
						}
					}.start();
				}
			}

			@Override
			public void onTick(long arg0) 
			{
				return;
			}
		}.start();
	}

	@SuppressWarnings("deprecation")
	private void playNotificationSound()
	{
		final String soundPath = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_key_ringtone", null);
		final boolean mediaOutput = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_key_media_output", false);

		if (soundPath != null && !soundPath.equals("")) 
		{
			final Uri soundUri = Uri.parse(soundPath);
			if (soundUri != null) 
			{
				if (_sfx != null)
					_sfx.stop();

				_sfx = RingtoneManager.getRingtone(this, soundUri);
				if (_sfx != null) 
				{
					int streamType = mediaOutput ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_NOTIFICATION;
					_sfx.setStreamType(streamType);
					_sfx.play();
				}
			}
		}
	}

	private void vibrate()
	{
		_vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		long[] pattern = new long[] { 0, 1250, 0 };
		_vibrator.vibrate(pattern, -1);
	}
}
