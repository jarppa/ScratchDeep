package com.brillenheini.deepscratch;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

public final class RecordPlayer {

	public static final int AUDIOTRACK_NORMAL=0;
	public static final int AUDIOTRACK_FAST=1;
	public static final int AUDIOTRACK_SLOW=2;
	public static final int AUDIOTRACK_NUM=3;
	
	private Vector<Recording> mSelection = new Vector<Recording>(5);
	
	private class PlayRecordTask extends AsyncTask<Recording, Integer, Long> {
	     protected Long doInBackground(Recording... recs) {
	         long totalSize = 0;
	         recs[0].getAudioTrack().write(recs[0].getData(),0,recs[0].getLen());
	         return totalSize;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	     }

	     protected void onPostExecute(Long result) {
	    	 
	     }
	 }
	
	public RecordPlayer() {
		mSelection.setSize(5);
	}
	
	public void playSample(int id) {
		Recording rec = mSelection.get(id-1);
		if (rec == null)
			return;
		
		new PlayRecordTask().execute(rec);
	}
	
	public void setSampleSpeed(int id, int s) { //FIXME: error handling
		mSelection.get(id).setAudioTrackSpeed(s);
	}
	
	public void addSample(String path, String name, int loc) {
		//Add new sample to selection
		
		//create new AudioTrack for the sample
		File file = new File(path);
		int length = (int)(file.length());
		
		if (length<1)
			return;
		
		short [] record = new short[length];
		
		AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 
				16000, 
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, 
				length, 
				AudioTrack.MODE_STREAM);
		AudioTrack fast = new AudioTrack(AudioManager.STREAM_MUSIC, 
				20000, 
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, 
				length, 
				AudioTrack.MODE_STREAM);
		AudioTrack slow = new AudioTrack(AudioManager.STREAM_MUSIC, 
				12000, 
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, 
				length, 
				AudioTrack.MODE_STREAM);
		
		try {
			// Create a DataInputStream to read the audio data back from the saved file.
			InputStream is = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			DataInputStream dis = new DataInputStream(bis);

			// Read the file into the music array.
			int i = 0;
			while (dis.available() > 0) {
				record[i] = dis.readShort();
				i++;
			}
			// Close the input streams.
			dis.close(); 
		}catch (FileNotFoundException fe){fe.printStackTrace();}catch (IOException ioe){ioe.printStackTrace();}
		
		at.play();
				
		mSelection.setElementAt(new Recording(name,record,length,at,fast,slow),loc-1);
	}
	
	private class Recording {
		private AudioTrack at;
		private AudioTrack fast_at;
		private AudioTrack slow_at;
		private short [] data;
		private String name;
		private int len;
		private AudioTrack [] tracks = {at,fast_at,slow_at};
		private AudioTrack selectedTrack = at;
		
		public Recording(String name, short [] data, int length, AudioTrack audiotrack, AudioTrack fast, AudioTrack slow)
		{
			at=audiotrack;
			fast_at = fast;
			slow_at = slow;
			this.data=data;
			this.name=name;
			len = length;
		}
		public int getLen()
		{
			return len;
		}
		public String getName()
		{
			return name;
		}
		public short [] getData()
		{
			return data;
		}
		public AudioTrack getAudioTrack()
		{
			return selectedTrack;
		}
		public void setAudioTrackSpeed(int s)
		{
			if (s < AUDIOTRACK_NUM)
				selectedTrack=tracks[s];
		}
	}
}
