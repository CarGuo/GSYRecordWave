package com.piterwilson.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.BaseRecorder;
import com.shuyu.waveview.Manager;

import static android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC;

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

    private int seekOffset = 0;
    //波形速度
    private int mWaveSpeed = 300;

    private boolean seekOffsetFlag = false;

    private boolean isLoop = false;

    private boolean hadPlay = false;

    private boolean pause;

    private long duration;

    private long curPosition;

    private long startWaveTime = 0;


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
        Pause,
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


    public void setUrlString(String urlString) {
        setUrlString(null, false, urlString);
    }

    /**
     * add cache url when play
     */
    public void setUrlString(Context context, boolean cache, String urlString) {
        String url = urlString;
        if (context != null && cache && !TextUtils.isEmpty(urlString) && urlString.startsWith("http")) {
            url = Manager.newInstance().getProxy(context).getProxyUrl(urlString);
        }
        this.mUrlString = url;
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
        if(startWaveTime > 0) {
            seekOffsetFlag = true;
        }
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

        // 这里配置一个路径文件
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(this.mUrlString);
        } catch (Exception e) {
            mDelegateHandler.onRadioPlayerError(MP3RadioStreamPlayer.this);
            return;
        }

        //获取多媒体文件信息
        MediaFormat format = extractor.getTrackFormat(0);
        //媒体类型
        String mime = format.getString(MediaFormat.KEY_MIME);

        // 检查是否为音频文件
        if (!mime.startsWith("audio/")) {
            Log.e("MP3RadioStreamPlayer", "不是音频文件!");
            return;
        }

        // 声道个数：单声道或双声道
        int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // if duration is 0, we are probably playing a live stream

        //时长
        duration = format.getLong(MediaFormat.KEY_DURATION);
        // System.out.println("歌曲总时间秒:"+duration/1000000);

        //时长
        //int bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);

        // the actual decoder
        try {
            // 实例化一个指定类型的解码器,提供数据输出
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        // 用来存放目标文件的数据
        codecInputBuffers = codec.getInputBuffers();
        // 解码后的数据
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);


        // 设置声道类型:AudioFormat.CHANNEL_OUT_MONO单声道，AudioFormat.CHANNEL_OUT_STEREO双声道
        int channelConfiguration = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        //Log.i(TAG, "channelConfiguration=" + channelConfiguration);

        Log.i(LOG_TAG, "mime " + mime);
        Log.i(LOG_TAG, "sampleRate " + sampleRate);

        // create our AudioTrack instance
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfiguration,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate,
                        channelConfiguration,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        //开始play，等待write发出声音
        audioTrack.play();
        extractor.selectTrack(0);//选择读取音轨

        // start decoding
        final long kTimeOutUs = 10000;//超时
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        // 解码
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;

        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !doStop) {

            if (pause) {
                this.mState = State.Pause;
                try {
                    //防止死循环ANR
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            noOutputCounter++;
            if (!sawInputEOS) {
                if (seekOffsetFlag) {
                    seekOffsetFlag = false;
                    long seek = (startWaveTime > seekOffset) ? startWaveTime : seekOffset;
                    extractor.seekTo(seek, SEEK_TO_PREVIOUS_SYNC);
                }

                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                bufIndexCheck++;
                //Log.e(LOG_TAG, " inputBufIndex " + inputBufIndex);
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
                    curPosition = presentationTimeUs;
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

            // decode to PCM and push it to the AudioTrack player
            // 解码数据为PCM
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
                if (chunk.length > 0 && audioTrack != null && !doStop) {
                    //播放
                    audioTrack.write(chunk, 0, chunk.length);

                    //根据数据的大小为把byte合成short文件
                    //然后计算音频数据的音量用于判断特征
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
                //释放
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
                    return;
                }
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
            if (!doStop)
                audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
    }

    /**
     * Stops playback
     */
    public void stop() {
        pause = false;
        doStop = true;
        seekOffset = 0;
        seekOffsetFlag = false;
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
            if(getCurPosition() >= startWaveTime) {
                int length = readSize / mWaveSpeed;
                short resultMax = 0, resultMin = 0;
                for (short i = 0, k = 0; i < length; i++, k += mWaveSpeed) {
                    for (short j = k, max = 0, min = 1000; j < k + mWaveSpeed; j++) {
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

    /**
     * 循环
     */
    public void setLoop(boolean loop) {
        isLoop = loop;
    }


    /**
     * 时长
     */
    public long getDuration() {
        return duration;
    }

    public boolean isPause() {
        return pause;
    }

    /**
     * 暂停
     */
    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void seekTo(int time) {
        if (time >= duration || pause) {
            return;
        }
        stop();
        seekOffsetFlag = true;
        this.seekOffset = time;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    play();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 300);
    }


    public long getCurPosition() {
        return curPosition;
    }

    public long getStartWaveTime() {
        return startWaveTime;
    }

    /**
     * 设置开始绘制波形的启始时间,播放前设置，不会被清空
     * @param startWaveTime 毫秒
     * */
    public void setStartWaveTime(long startWaveTime) {
        this.startWaveTime = startWaveTime * 1000;
    }

    /**
     * pcm数据的速度，默认300
     * 数据越大，速度越慢
     */
    public void setWaveSpeed(int waveSpeed) {
        if (mWaveSpeed <= 0) {
            return;
        }
        this.mWaveSpeed = waveSpeed;
    }
}
