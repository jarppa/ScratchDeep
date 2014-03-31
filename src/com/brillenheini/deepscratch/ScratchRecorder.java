package com.brillenheini.deepscratch;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.brillenheini.deepscratch.log.LL;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Environment;

/*
 * Thread to manage live recording/playback of voice input from the device's microphone.
 */
public class ScratchRecorder extends Thread
{
    private boolean stopped = false;
    private RecordObserver observer = null;
    private AudioRecord mRecorder = null;
    private boolean mRecording = false;
    private DataOutputStream dos;
    private File file = null;
    private int mId = 0;
    private static final long maxRecordTime = 10000;
    
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public ScratchRecorder(String name)
    {
    	super(name);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    }

    @Override
    public void run()
    { 
        LL.info("Audio thread start");
        boolean firstBurst = true;
        long starttime = 0;
        long elapsed = 0;
        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            int size = AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if (size < 1)
            {
            	LL.error("Less than one sized buffer. Cannot continue!");
            	return;
            }

            mRecorder = new AudioRecord(AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size*2);
            short[] buffer = new short[size*2];
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(!stopped)
            {
            	if (mRecording)
            	{
            		if (firstBurst) {
            			starttime = System.currentTimeMillis();
            			elapsed = 0;
            		}
            		else {
            			elapsed = System.currentTimeMillis()-starttime;
            		}
            		
            		if (elapsed > maxRecordTime) {
            			continue; //Just ignore everything following. Let the user stop the recording.
            		}
            		
            		LL.info("Writing new data to buffer");
            		//ix = ix++ % mRecBuffer.length;
            		//= mRecBuffer[mRecBuffer.length-ix];
            		int num = 0;
            		synchronized(mRecorder)
            		{
            			num = mRecorder.read(buffer,0,buffer.length);
            		}
            		if (firstBurst)
            		{
            			firstBurst = false;
            			short [] modbuffer = cutSilence(buffer,100);
            			for (int i = 0; i < modbuffer.length; i++)
                    		dos.writeShort(modbuffer[i]);
            		}
            		else
            		{
            			for (int i = 0; i < num; i++)
            				dos.writeShort(buffer[i]);
            		}
            		
            		for (int i=0; i<buffer.length;i++)
            		{
            			buffer[i]=0;
            		}
            	}
            	else
            		firstBurst=true;
            }
        }
        catch(Throwable x)
        {
        	x.printStackTrace();
            LL.warn("Error reading voice audio");
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        {
        	if(mRecording)
        	{
        		mRecorder.stop();
        		mRecording = false;
        	}
            mRecorder.release();
            mRecorder = null;
        }
    }
    
    public void startRecord(int id)
    {
    	if (!mRecording)
    	{
    		mId = id;
    		file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record"+String.valueOf(id)+".pcm");
    		// Delete any previous recording.
    		if (file.exists())
    			file.delete();

    		// Create the new file.
    		try {
    			file.createNewFile();
    		} catch (IOException e) {
    			throw new IllegalStateException("Failed to create " + file.toString());
    		}
    		OutputStream os;
			try {
				os = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(os);
	    		dos = new DataOutputStream(bos);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
    		mRecorder.startRecording();
    		mRecording = true;
    	}
    }
    
    public boolean isRecording()
    {
    	return mRecording;
    }
    
    public void stopRecord()
    {
    	if(mRecording)
    	{
    		mRecording = false;
    		synchronized(mRecorder)
    		{
    			mRecorder.stop();
    		}
    		try {
				dos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		if (observer != null)
            {
            	observer.notifyRecordChanged(file.getPath(),mId);
            }
    	}
    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    public void close()
    { 
         stopped = true;
    }
    
    public void AttachObserver(RecordObserver ob)
    {
    	observer = ob;
    }
    
    private short [] cutSilence(short [] buf, int threshold)
    {
    	short [] ret;
    	int startpos = 0, endpos=0;
    	
    	for (int i=0;i<buf.length;i++)
    	{
    		if (buf[i] > threshold || buf[i] < -(threshold))
    		{
    			startpos = i;
    			break;
    		}
    	}
    	for (int i=buf.length-1; i>startpos; i--)
    	{
    		if (buf[i] > threshold || buf[i] < -(threshold))
    		{
    			endpos=i;
    			break;
    		}
    	}
    	
    	if (startpos > 0)
    	{
    		ret = new short[endpos-startpos];
    		for(int i=0,j=startpos; j<endpos; j++,i++)
    		{
    			ret[i]=buf[j];
    		}
    		return ret;
    	}
    	else
    	{
    		return new short[0];
    	}
    }
}