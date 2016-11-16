package com.piterwilson.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.BaseRecorder;

/**
 * Plays a MP3 Radio stream using MediaExtractor, MediaCodec and AudioTrack
 *
 * @author Juan Carlos Ospina Gonzalez / juan@supersteil.com
 */

public class MP3RadioStreamPlayer extends BaseRecorder {

    public final String LOG_TAG = "MP3RadioStreamPlayer";

    protected MediaExtractor extractor;
    protected MediaCodec codec;
    protected AudioTrack audioTrack;

    protected int inputBufIndex;
    protected int bufIndexCheck;
    protected int lastInputBufIndex;

    protected Boolean doStop = false;
    /*
     * Delegate to receive notifications
     */
    protected MP3RadioStreamDelegate mDelegate;


    private ArrayList<Short> dataList;

    private int maxSize;

    private boolean isLoop = false;

    private boolean hadPlay = false;

    /**
     * Set the delegate for this instance. The delegate will receive notifications about the player's status
     *
     * @param mDelegate
     */
    public void setDelegate(MP3RadioStreamDelegate mDelegate) {
        this.mDelegate = mDelegate;
    }

    public MP3RadioStreamDelegate getDelegate() {
        return this.mDelegate;
    }

    // indicates the state our service:
    public enum State {
        Retrieving, // retrieving music (filling buffer)
        Stopped,    // player is stopped and not prepared to play
        Playing,    // playback active 
    }

    ;

    /**
     * Current player state
     */
    State mState = State.Retrieving;

    /**
     * Getter for mState
     */
    public State getState() {
        return mState;
    }

    /**
     * String The url of the radio stream
     */
    private String mUrlString;

    public void setUrlString(String mUrlString) {
        this.mUrlString = mUrlString;
    }

    /**
     * mUrlString getter
     */
    public String getUrlString() {
        return mUrlString;
    }

    /**
     * Constructor
     */
    public MP3RadioStreamPlayer() {
        mState = State.Stopped;
    }

    /**
     * Attempts to fetch mp3 data from the mUrlString location, decode it and feed it to an AudioTrack instance
     *
     * @throws IOException
     */
    public void play() throws IOException {
        mState = State.Retrieving;
        mDelegateHandler.onRadioPlayerBuffering(MP3RadioStreamPlayer.this);
        doStop = false;
        bufIndexCheck = 0;
        lastInputBufIndex = -1;

        myTimerTask = new CheckProgressTimerTask();
        myTimer = new Timer();
        myTimer.scheduleAtFixedRate(myTimerTask, 0, 1000); //(timertask,delay,period)

        new DecodeOperation().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private DelegateHandler mDelegateHandler = new DelegateHandler();

    class DelegateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        }

        public void onRadioPlayerPlaybackStarted(MP3RadioStreamPlayer player) {
            if (MP3RadioStreamPlayer.this.mDelegate != null) {
                MP3RadioStreamPlayer.this.mDelegate.onRadioPlayerPlaybackStarted(player);
            }
        }

        public void onRadioPlayerStopped(MP3RadioStreamPlayer player) {
            if (MP3RadioStreamPlayer.this.mDelegate != null) {
                MP3RadioStreamPlayer.this.mDelegate.onRadioPlayerStopped(player);
            }
        }

        public void onRadioPlayerError(MP3RadioStreamPlayer player) {
            if (MP3RadioStreamPlayer.this.mDelegate != null) {
                MP3RadioStreamPlayer.this.mDelegate.onRadioPlayerError(player);
            }
        }

        public void onRadioPlayerBuffering(MP3RadioStreamPlayer player) {
            if (MP3RadioStreamPlayer.this.mDelegate != null) {
                MP3RadioStreamPlayer.this.mDelegate.onRadioPlayerBuffering(player);
            }
        }
    }

    ;

    Timer myTimer;
    CheckProgressTimerTask myTimerTask;

    private class CheckProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (lastInputBufIndex == bufIndexCheck) {
                Log.d(LOG_TAG, "----lastInputBufIndex " + lastInputBufIndex);
                Log.d(LOG_TAG, "----bufIndexCheck " + bufIndexCheck);

                // buferring ... (buffer has not progressed)
                if (MP3RadioStreamPlayer.this.mState == State.Playing) {
                    Log.d(LOG_TAG, "buffering???? onRadioPlayerBuffering");
                    mDelegateHandler.onRadioPlayerBuffering(MP3RadioStreamPlayer.this);
                }

                MP3RadioStreamPlayer.this.mState = State.Retrieving;
            }
            lastInputBufIndex = bufIndexCheck;
            Log.d(LOG_TAG, "lastInputBufIndex " + lastInputBufIndex);
            if (bufIndexCheck > 9999) {
                bufIndexCheck = 0;
            }
        }
    }

    /**
     * @throws IOException
     */
    private void decodeLoop() {

        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        // extractor gets information about the stream
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(this.mUrlString);
        } catch (Exception e) {
            mDelegateHandler.onRadioPlayerError(MP3RadioStreamPlayer.this);
            return;
        }

        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);

        // the actual decoder
        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        Log.i(LOG_TAG, "mime " + mime);
        Log.i(LOG_TAG, "sampleRate " + sampleRate);

        // create our AudioTrack instance
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // start playing, we will feed you later
        audioTrack.play();
        extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;


        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !doStop) {
            //Log.i(LOG_TAG, "loop ");
            noOutputCounter++;
            if (!sawInputEOS) {

                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                bufIndexCheck++;
                // Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
                        Log.d(LOG_TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)

                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);


                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e(LOG_TAG, "inputBufIndex " + inputBufIndex);
                }
            }

            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                //Log.d(LOG_TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if (chunk.length > 0) {
                    audioTrack.write(chunk, 0, chunk.length);

                    short[] music = (!isBigEnd()) ? byteArray2ShortArrayLittle(chunk, chunk.length / 2) :
                            byteArray2ShortArrayBig(chunk, chunk.length / 2);
                    sendData(music, music.length);
                    calculateRealVolume(music, music.length);

                    if (this.mState != State.Playing) {
                        mDelegateHandler.onRadioPlayerPlaybackStarted(MP3RadioStreamPlayer.this);
                    }
                    this.mState = State.Playing;
                    hadPlay = true;
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        Log.d(LOG_TAG, "stopping...");

        relaxResources(true);

        this.mState = State.Stopped;
        doStop = true;

        // attempt reconnect
        if (sawOutputEOS) {
            try {
                if (isLoop || !hadPlay) {
                    MP3RadioStreamPlayer.this.play();
                }
                return;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (noOutputCounter >= noOutputCounterLimit) {
            mDelegateHandler.onRadioPlayerError(MP3RadioStreamPlayer.this);
        } else {
            mDelegateHandler.onRadioPlayerStopped(MP3RadioStreamPlayer.this);
        }
    }

    public void release() {
        stop();
        relaxResources(false);
    }

    private void relaxResources(Boolean release) {
        if (codec != null) {
            if (release) {
                codec.stop();
                codec.release();
                codec = null;
            }

        }
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
    }

    /**
     * Stops playback
     */
    public void stop() {
        doStop = true;
        if (myTimer != null) {
            myTimer.cancel();
            myTimer = null;
        }
        if (myTimerTask != null) {
            myTimerTask.cancel();
            myTimerTask = null;
        }
    }

    /**
     * AsyncTask that takes care of running the decode/playback loop
     */
    private class DecodeOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... values) {
            MP3RadioStreamPlayer.this.decodeLoop();
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    private boolean isBigEnd() {
        short i = 0x1;
        boolean bRet = ((i >> 8) == 0x1);
        return bRet;
    }

    private short[] byteArray2ShortArrayBig(byte[] data, int items) {
        short[] retVal = new short[items];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = (short) ((data[i * 2 + 1] & 0xff) | (data[i * 2] & 0xff) << 8);

        return retVal;
    }

    private short[] byteArray2ShortArrayLittle(byte[] data, int items) {
        short[] retVal = new short[items];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] & 0xff) << 8);

        return retVal;
    }

    private void sendData(short[] shorts, int readSize) {
        if (dataList != null) {
            int length = readSize / 300;
            short resultMax = 0, resultMin = 0;
            for (short i = 0, k = 0; i < length; i++, k += 300) {
                for (short j = k, max = 0, min = 1000; j < k + 300; j++) {
                    if (shorts[j] > max) {
                        max = shorts[j];
                        resultMax = max;
                    } else if (shorts[j] < min) {
                        min = shorts[j];
                        resultMin = min;
                    }
                }
                if (dataList.size() > maxSize) {
                    dataList.remove(0);
                }
                dataList.add(resultMax);
            }
        }
    }



    /**
     * 获取真实的音量。 [算法来自三星]
     *
     * @return 真实音量
     */
    @Override
    public int getRealVolume() {
        return mVolume;
    }

    /**
     * 设置数据的获取显示，设置最大的获取数，一般都是控件大小/线的间隔offset
     *
     * @param dataList 数据
     * @param maxSize  最大个数
     */
    public void setDataList(ArrayList<Short> dataList, int maxSize) {
        this.dataList = dataList;
        this.maxSize = maxSize;
    }

    public boolean isLoop() {
        return isLoop;
    }

    public void setLoop(boolean loop) {
        isLoop = loop;
    }
}
