package com.czt.mp3recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;

import com.BaseRecorder;
import com.czt.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MP3Recorder extends BaseRecorder {
    //=======================AudioRecord Default Settings=======================
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     */
    private static final int DEFAULT_SAMPLING_RATE = 44100;//模拟器仅支持从麦克风输入8kHz采样率
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
     */
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

    //==================================================================

    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 160;
    public static final int ERROR_TYPE = 22;


    private AudioRecord mAudioRecord = null;
    private DataEncodeThread mEncodeThread;
    private File mRecordFile;
    private ArrayList<Short> dataList;
    private Handler errorHandler;


    private short[] mPCMBuffer;
    private boolean mIsRecording = false;
    private boolean mSendError;
    private boolean mPause;
    //缓冲数量
    private int mBufferSize;
    //最大数量
    private int mMaxSize;
    //波形速度
    private int mWaveSpeed = 300;

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     *
     * @param recordFile target file
     */
    public MP3Recorder(File recordFile) {
        mRecordFile = recordFile;
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    public void start() throws IOException {
        if (mIsRecording) {
            return;
        }
        mIsRecording = true; // 提早，防止init或startRecording被多次调用
        initAudioRecorder();
        try {
            mAudioRecord.startRecording();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        new Thread() {
            boolean isError = false;

            @Override
            public void run() {
                //设置线程权限
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (mIsRecording) {
                    int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);


                    if (readSize == AudioRecord.ERROR_INVALID_OPERATION ||
                            readSize == AudioRecord.ERROR_BAD_VALUE) {
                        if (errorHandler != null && !mSendError) {
                            mSendError = true;
                            errorHandler.sendEmptyMessage(ERROR_TYPE);
                            mIsRecording = false;
                            isError = true;
                        }
                    } else {

                        if (readSize > 0) {
                            if (mPause) {
                                continue;
                            }
                            mEncodeThread.addTask(mPCMBuffer, readSize);
                            calculateRealVolume(mPCMBuffer, readSize);
                            sendData(mPCMBuffer, readSize);
                        } else {
                            if (errorHandler != null && !mSendError) {
                                mSendError = true;
                                errorHandler.sendEmptyMessage(ERROR_TYPE);
                                mIsRecording = false;
                                isError = true;
                            }
                        }
                    }
                }
                try {
                    // release and finalize audioRecord
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // stop the encoding thread and try to wait
                // until the thread finishes its job
                if (isError) {
                    mEncodeThread.sendErrorMessage();
                } else {
                    mEncodeThread.sendStopMessage();
                }
            }

        }.start();
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
     * 获取相对音量。 超过最大值时取最大值。
     *
     * @return 音量
     */
    public int getVolume() {
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    private static final int MAX_VOLUME = 2000;

    /**
     * 根据资料假定的最大值。 实测时有时超过此值。
     *
     * @return 最大音量值。
     */
    public int getMaxVolume() {
        return MAX_VOLUME;
    }

    public void stop() {
        mPause = false;
        mIsRecording = false;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
		 * 使能被整除，方便下面的周期性通知
		 * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }

		/* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
        /*
         * Initialize lame buffer
		 * mp3 sampling rate is the same as the recorded pcm sampling rate 
		 * The bit rate is 32kbps
		 * 
		 */
        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        // Create and run thread used to encode data
        // The thread will
        mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    private void sendData(short[] shorts, int readSize) {
        if (dataList != null) {
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
                if (dataList.size() > mMaxSize) {
                    dataList.remove(0);
                }
                dataList.add(resultMax);
            }
        }
    }

    /**
     * 设置数据的获取显示，设置最大的获取数，一般都是控件大小/线的间隔offset
     *
     * @param dataList 数据
     * @param maxSize  最大个数
     */
    public void setDataList(ArrayList<Short> dataList, int maxSize) {
        this.dataList = dataList;
        this.mMaxSize = maxSize;
    }


    public boolean isPause() {
        return mPause;
    }

    /**
     * 是否暂停
     */
    public void setPause(boolean pause) {
        this.mPause = pause;
    }

    /**
     * 设置错误回调
     *
     * @param errorHandler 错误通知
     */
    public void setErrorHandler(Handler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public int getWaveSpeed() {
        return mWaveSpeed;
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

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }

}