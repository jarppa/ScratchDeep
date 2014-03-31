/*
 * Deep Scratch for Android
 * Copyright (C) 2010, 2012 Stefan Schweizer
 *
 * This file is part of Deep Scratch.
 *
 * Deep Scratch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Deep Scratch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Deep Scratch.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.brillenheini.deepscratch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.brillenheini.deepscratch.free.R;
import com.brillenheini.deepscratch.log.LL;
import com.brillenheini.deepscratch.sound.Sample;
import com.brillenheini.deepscratch.sound.ScratchSoundPool;
import com.brillenheini.deepscratch.view.Converter;
import com.brillenheini.deepscratch.view.ScratchView;

/**
 * @author Stefan Schweizer
 */
public class DeepScratch extends Activity implements RecordObserver, OnClickListener, OnCheckedChangeListener{
	private static final int PICK_SONG = 1;
	private static final int ITEM_ID_SAMPLE = 1;
	protected static final int DIALOG_HELP = 1;

	private static final String STATE_SAMPLE = "STATE_SAMPLE";
	private static final String STATE_URI = "STATE_URI";
	private static final String STATE_POSITION = "STATE_POSITION";
	private static final String STATE_PAUSED = "STATE_PAUSED";

	private static final float MEDIA_VOLUME = 0.75f;

	// Sample and media playback, saved as instance state
	private int mSelectedSample = 0;
	private Uri mUri = null;
	private int mPosition = 0;
	private boolean mPaused = false;

	private List<Sample> mSamples;
	private ScratchSoundPool mSounds;
	private ScratchView mScratchView;
	private MediaPlayer mPlayer;

	private Button mButt1, mButt2, mButt3, mButt4, mButt5, mPlay1, mPlay2, mPlay3, mPlay4, mPlay5;
	private RadioGroup mRadioG1,mRadioG2,mRadioG3,mRadioG4,mRadioG5;
	
	private ScratchRecorder mRecorder;
	private RecordPlayer mRecordPlayer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Converter.initialize(this);
		setContentView(R.layout.main);
		mButt1 = (Button) findViewById(R.id.button1);
		if (mButt1 != null)
			mButt1.setOnClickListener(this);
		mButt2 = (Button) findViewById(R.id.button2);
		if (mButt2 != null)
			mButt2.setOnClickListener(this);
		mButt3 = (Button) findViewById(R.id.button3);
		if (mButt3 != null)
			mButt3.setOnClickListener(this);
		mButt4 = (Button) findViewById(R.id.button4);
		if (mButt4 != null)
			mButt4.setOnClickListener(this);
		mButt5 = (Button) findViewById(R.id.button5);
		if (mButt5 != null)
			mButt5.setOnClickListener(this);
		
		mPlay1 = (Button) findViewById(R.id.play1);
		if (mPlay1 != null)
			mPlay1.setOnClickListener(this);
		mPlay2 = (Button) findViewById(R.id.play2);
		if (mPlay2 != null)
			mPlay2.setOnClickListener(this);
		mPlay3 = (Button) findViewById(R.id.play3);
		if (mPlay3 != null)
			mPlay3.setOnClickListener(this);
		mPlay4 = (Button) findViewById(R.id.play4);
		if (mPlay4 != null)
			mPlay4.setOnClickListener(this);
		mPlay5 = (Button) findViewById(R.id.play5);
		if (mPlay5 != null)
			mPlay5.setOnClickListener(this);
		
		mRadioG1 = (RadioGroup) findViewById(R.id.RadioGroup1);
		if (mRadioG1 != null)
			mRadioG1.setOnCheckedChangeListener(this);
		mRadioG2 = (RadioGroup) findViewById(R.id.RadioGroup2);
		if (mRadioG2 != null)
			mRadioG2.setOnCheckedChangeListener(this);
		mRadioG3 = (RadioGroup) findViewById(R.id.RadioGroup3);
		if (mRadioG3 != null)
			mRadioG3.setOnCheckedChangeListener(this);
		mRadioG4 = (RadioGroup) findViewById(R.id.RadioGroup4);
		if (mRadioG4 != null)
			mRadioG4.setOnCheckedChangeListener(this);
		mRadioG5 = (RadioGroup) findViewById(R.id.RadioGroup5);
		if (mRadioG5 != null)
			mRadioG5.setOnCheckedChangeListener(this);
		
		// Try to restore instance state
		if (savedInstanceState != null) {
			mSelectedSample = savedInstanceState.getInt(STATE_SAMPLE);
			String uri = savedInstanceState.getString(STATE_URI);
			if (uri != null) {
				mUri = Uri.parse(uri);
				mPosition = savedInstanceState.getInt(STATE_POSITION);
				mPaused = savedInstanceState.getBoolean(STATE_PAUSED);
			}
			if (LL.isDebugEnabled())
				LL.debug("Restoring: sample=" + mSelectedSample + " uri=" + uri
						+ " position=" + mPosition + " paused=" + mPaused);
		}
		mSamples = new ArrayList<Sample>();
		addSamples(mSamples);

		mSounds = new ScratchSoundPool();
		mSounds.loadSample(this, mSamples.get(mSelectedSample));

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mScratchView = (ScratchView) findViewById(R.id.scratch);
		mScratchView.setScratchSoundPool(mSounds);
		
		mRecordPlayer = new RecordPlayer();
	}

	/**
	 * Add available samples. The first sample is loaded on startup.
	 */
	private void addSamples(List<Sample> l) {
		l.add(new Sample("Uuh", R.raw.uuh, R.raw.uuh_fw, R.raw.uuh_bw));
		l.add(new Sample("Bass", R.raw.bass, R.raw.bass_fw, R.raw.bass_bw));
		l.add(new Sample("Fresh", R.raw.fresh, R.raw.fresh_fw, R.raw.fresh_bw));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SAMPLE, mSelectedSample);
		if (mUri != null) {
			outState.putString(STATE_URI, mUri.toString());
			outState.putInt(STATE_POSITION, mPlayer.getCurrentPosition());
			outState.putBoolean(STATE_PAUSED, mPaused);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mUri != null)
			preparePlayer();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mScratchView.startRotation();
		if (mPlayer != null && !mPaused)
			mPlayer.start();
		if (mRecorder == null)
		{
			mRecorder = new ScratchRecorder("rec");
			mRecorder.AttachObserver(this);
			mRecorder.start();
		}
			
	}

	@Override
	protected void onPause() {
		super.onPause();
		mScratchView.stopRotation();
		if (mPlayer != null && mPlayer.isPlaying())
			mPlayer.pause();
		if (mRecorder.isRecording())
			mRecorder.stopRecord();
		mRecorder.close();
		mRecorder = null;
	}

	@Override
	protected void onStop() {
		super.onStop();
		closePlayer();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mSounds.close();
		if (mRecorder != null)
		{
			mRecorder.stopRecord();
			mRecorder.close();
			mRecorder = null;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_HELP:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.help);
			dialog.setTitle(R.string.app_name);

			// Make links clickable
			TextView text = (TextView) dialog.findViewById(R.id.help_thanks);
			text.setMovementMethod(LinkMovementMethod.getInstance());
			text = (TextView) dialog.findViewById(R.id.help_license);
			text.setMovementMethod(LinkMovementMethod.getInstance());
			text = (TextView) dialog.findViewById(R.id.help_attributions);
			text.setMovementMethod(LinkMovementMethod.getInstance());
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.options_menu, menu);

		// Add samples to submenu
		if (mSamples.size() > 1) {
			MenuItem menuSample = menu.findItem(R.id.menu_sample);
			menuSample.setVisible(true);
			SubMenu subMenuSample = menuSample.getSubMenu();
			for (Sample sample : mSamples)
				subMenuSample.add(R.id.menu_sample_group, ITEM_ID_SAMPLE,
						Menu.NONE, sample.getName());
			subMenuSample.setGroupCheckable(R.id.menu_sample_group, true, true);
			subMenuSample.getItem(mSelectedSample).setChecked(true);
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (mPlayer != null) {
			menu.findItem(R.id.menu_pause).setVisible(!mPaused);
			menu.findItem(R.id.menu_play).setVisible(mPaused);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_music:
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("audio/*");
			try {
				startActivityForResult(intent, PICK_SONG);
			} catch (ActivityNotFoundException anfe) {
				toastError(R.string.error_noactivity, anfe);
			}
			return true;
		case R.id.menu_pause:
			mPlayer.pause();
			mPaused = true;
			return true;
		case R.id.menu_play:
			mPlayer.start();
			mPaused = false;
			return true;
		case R.id.menu_help:
			showDialog(DIALOG_HELP);
			return true;
		case ITEM_ID_SAMPLE:
			if (!item.isChecked()) {
				item.setChecked(true);
				mSelectedSample = Sample.findSample(mSamples, item.getTitle()
						.toString());
				mSounds.loadSample(this, mSamples.get(mSelectedSample));
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICK_SONG:
			if (resultCode == RESULT_OK) {
				mUri = data.getData();
				mPosition = 0;
				mPaused = false;
				preparePlayer();
			}
			break;
		}
	}

	protected void toastError(int id, Throwable tr) {
		CharSequence msg = getResources().getText(id);
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		LL.error(msg.toString(), tr);
	}

	private void preparePlayer() {
		MediaPlayer player = mPlayer;
		if (player == null) {
			player = new MediaPlayer();
		} else {
			player.reset();
		}
		try {
			player.setDataSource(this, mUri);
			player.prepare();
			player.setLooping(true);
			player.setVolume(MEDIA_VOLUME, MEDIA_VOLUME);
			if (mPosition > 0)
				player.seekTo(mPosition);
		} catch (IOException ioe) {
			LL.error("Error starting playback of " + mUri, ioe);
		} finally {
			mPlayer = player;
		}
	}

	private void closePlayer() {
		if (mPlayer != null) {
			mPosition = mPlayer.getCurrentPosition();
			mPlayer.release();
			mPlayer = null;
		}
	}

	@Override
	public void notifyRecordChanged(String path, int id) {
		//mSamples.add(new Sample("User", path));
		mRecordPlayer.addSample(path, "User Sample", id);
	}

	@Override
	public void onClick(View arg0) {
		if (arg0 == mButt1)
		{
			if(!mRecorder.isRecording())
			{
				mRecorder.startRecord(1);
				mButt1.setText("Stop");
			}
			else
			{
				mRecorder.stopRecord();
				mButt1.setText("Record 1");
			}
		
		}
		else if (arg0 == mButt2)
		{
			if(!mRecorder.isRecording())
			{
				mRecorder.startRecord(2);
				mButt2.setText("Stop");
			}
			else
			{
				mRecorder.stopRecord();
				mButt2.setText("Record 2");
			}
		
		}
		else if (arg0 == mButt3)
		{
			if(!mRecorder.isRecording())
			{
				mRecorder.startRecord(3);
				mButt3.setText("Stop");
			}
			else
			{
				mRecorder.stopRecord();
				mButt3.setText("Record 3");
			}
		
		}
		else if (arg0 == mButt4)
		{
			if(!mRecorder.isRecording())
			{
				mRecorder.startRecord(4);
				mButt4.setText("Stop");
			}
			else
			{
				mRecorder.stopRecord();
				mButt4.setText("Record 4");
			}
		
		}
		else if (arg0 == mButt5)
		{
			if(!mRecorder.isRecording())
			{
				mRecorder.startRecord(5);
				mButt5.setText("Stop");
			}
			else
			{
				mRecorder.stopRecord();
				mButt5.setText("Record 5");
			}
		
		}
		if (arg0 == mPlay1)
		{
			mRecordPlayer.playSample(1);
		}
		else if (arg0 == mPlay2)
		{
			mRecordPlayer.playSample(2);
		}
		else if (arg0 == mPlay3)
		{
			mRecordPlayer.playSample(3);
		}
		else if (arg0 == mPlay4)
		{
			mRecordPlayer.playSample(4);
		}
		else if (arg0 == mPlay5)
		{
			mRecordPlayer.playSample(5);
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup arg0, int arg1) {
		if (arg0 == mRadioG1) {
			switch(arg1) {
			case R.id.RadioButton1_1:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_SLOW);
				break;
			case R.id.RadioButton1_2:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_NORMAL);
				break;
			case R.id.RadioButton1_3:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_FAST);
				break;
			default:
				break;
			}
		}
		else if (arg0 == mRadioG2) {
			switch(arg1) {
			case R.id.RadioButton2_1:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_SLOW);
				break;
			case R.id.RadioButton2_2:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_NORMAL);
				break;
			case R.id.RadioButton2_3:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_FAST);
				break;
			default:
				break;
			}
		}
		else if (arg0 == mRadioG3) {
			switch(arg1) {
			case R.id.RadioButton3_1:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_SLOW);
				break;
			case R.id.RadioButton3_2:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_NORMAL);
				break;
			case R.id.RadioButton3_3:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_FAST);
				break;
			default:
				break;
			}	
		}
		else if (arg0 == mRadioG4) {
			switch(arg1) {
			case R.id.RadioButton4_1:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_SLOW);
				break;
			case R.id.RadioButton4_2:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_NORMAL);
				break;
			case R.id.RadioButton4_3:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_FAST);
				break;
			default:
				break;
			}
		}
		else if (arg0 == mRadioG5) {
			switch(arg1) {
			case R.id.RadioButton5_1:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_SLOW);
				break;
			case R.id.RadioButton5_2:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_NORMAL);
				break;
			case R.id.RadioButton5_3:
				mRecordPlayer.setSampleSpeed(1, RecordPlayer.AUDIOTRACK_FAST);
				break;
			default:
				break;
			}
		}
	}
}