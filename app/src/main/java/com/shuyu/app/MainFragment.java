package com.shuyu.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.shuyu.waveview.AudioPlayer;
import com.shuyu.waveview.AudioWaveView;
import com.shuyu.waveview.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by shuyu on 2016/12/16.
 */

public class MainFragment extends Fragment {
    @BindView(R.id.audioWave)
    AudioWaveView audioWave;
    @BindView(R.id.record)
    Button record;
    @BindView(R.id.stop)
    Button stop;
    @BindView(R.id.play)
    Button play;
    @BindView(R.id.reset)
    Button reset;
    @BindView(R.id.wavePlay)
    Button wavePlay;
    @BindView(R.id.playText)
    TextView playText;
    @BindView(R.id.colorImg)
    ImageView colorImg;
    @BindView(R.id.recordPause)
    Button recordPause;


    MP3Recorder mRecorder;
    AudioPlayer audioPlayer;

    String filePath;

    boolean mIsRecord = false;

    boolean mIsPlay = false;

    int duration;
    int curPosition;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resolveNormalUI();

        audioPlayer = new AudioPlayer(getActivity(), new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case AudioPlayer.HANDLER_CUR_TIME://更新的时间
                        curPosition = (int) msg.obj;
                        playText.setText(toTime(curPosition) + " / " + toTime(duration));
                        break;
                    case AudioPlayer.HANDLER_COMPLETE://播放结束
                        playText.setText(" ");
                        mIsPlay = false;
                        break;
                    case AudioPlayer.HANDLER_PREPARED://播放开始
                        duration = (int) msg.obj;
                        playText.setText(toTime(curPosition) + " / " + toTime(duration));
                        break;
                    case AudioPlayer.HANDLER_ERROR://播放错误
                        resolveResetPlay();
                        break;
                }

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mIsRecord) {
            resolveStopRecord();
        }
        if (mIsPlay) {
            audioPlayer.pause();
            audioPlayer.stop();
        }
    }

    @OnClick({R.id.record, R.id.stop, R.id.play, R.id.reset, R.id.wavePlay, R.id.recordPause})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record:
                resolveRecord();
                break;
            case R.id.stop:
                resolveStopRecord();
                break;
            case R.id.play:
                resolvePlayRecord();
                break;
            case R.id.reset:
                resolveResetPlay();
            case R.id.wavePlay:
                resolvePlayWaveRecord();
            case R.id.recordPause:
                resolvePause();
                break;
        }
    }

    /**
     * 开始录音
     */
    private void resolveRecord() {
        filePath = FileUtils.getAppPath();
        File file = new File(filePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(getActivity(), "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int offset = dip2px(getActivity(), 5);
        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        mRecorder = new MP3Recorder(new File(filePath));
        int size = getScreenWidth(getActivity()) / offset;//控件默认的间隔是1
        mRecorder.setDataList(audioWave.getRecList(), size);

        //高级用法
        //int size = (getScreenWidth(getActivity()) / 2) / dip2px(getActivity(), 1);
        //mRecorder.setWaveSpeed(600);
        //mRecorder.setDataList(audioWave.getRecList(), size);
        //audioWave.setDrawStartOffset((getScreenWidth(getActivity()) / 2));
        //audioWave.setDrawReverse(true);
        //audioWave.setDataReverse(true);

        //自定义paint
        //Paint paint = new Paint();
        //paint.setColor(Color.GRAY);
        //paint.setStrokeWidth(4);
        //audioWave.setLinePaint(paint);
        //audioWave.setOffset(offset);

        mRecorder.setErrorHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(getActivity(), "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    resolveError();
                }
            }
        });

        //audioWave.setBaseRecorder(mRecorder);

        try {
            mRecorder.start();
            audioWave.startView();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "录音出现异常", Toast.LENGTH_SHORT).show();
            resolveError();
            return;
        }
        resolveRecordUI();
        mIsRecord = true;
    }

    /**
     * 停止录音
     */
    private void resolveStopRecord() {
        resolveStopUI();
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
            audioWave.stopView();
        }
        mIsRecord = false;
        recordPause.setText("暂停");

    }

    /**
     * 录音异常
     */
    private void resolveError() {
        resolveNormalUI();
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWave.stopView();
        }
    }

    /**
     * 播放
     */
    private void resolvePlayRecord() {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(getActivity(), "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        playText.setText(" ");
        mIsPlay = true;
        audioPlayer.playUrl(filePath);
        resolvePlayUI();
    }

    /**
     * 播放
     */
    private void resolvePlayWaveRecord() {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(getActivity(), "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        resolvePlayUI();
        Intent intent = new Intent(getActivity(), WavePlayActivity.class);
        intent.putExtra("uri", filePath);
        startActivity(intent);
    }

    /**
     * 重置
     */
    private void resolveResetPlay() {
        filePath = "";
        playText.setText("");
        if (mIsPlay) {
            mIsPlay = false;
            audioPlayer.pause();
        }
        resolveNormalUI();
    }

    /**
     * 暂停
     */
    private void resolvePause() {
        if (!mIsRecord)
            return;
        resolvePauseUI();
        if (mRecorder.isPause()) {
            resolveRecordUI();
            mRecorder.setPause(false);
            recordPause.setText("暂停");
        } else {
            mRecorder.setPause(true);
            recordPause.setText("继续");
        }
    }

    private String toTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        String dateString = formatter.format(time);
        return dateString;
    }

    private void resolveNormalUI() {
        record.setEnabled(true);
        recordPause.setEnabled(false);
        stop.setEnabled(false);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveRecordUI() {
        record.setEnabled(false);
        recordPause.setEnabled(true);
        stop.setEnabled(true);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveStopUI() {
        record.setEnabled(true);
        stop.setEnabled(false);
        recordPause.setEnabled(false);
        play.setEnabled(true);
        wavePlay.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolvePlayUI() {
        record.setEnabled(false);
        stop.setEnabled(false);
        recordPause.setEnabled(false);
        play.setEnabled(true);
        wavePlay.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolvePauseUI() {
        record.setEnabled(false);
        recordPause.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        wavePlay.setEnabled(false);
        reset.setEnabled(false);
    }


    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    /**
     * 获取屏幕的高度px
     *
     * @param context 上下文
     * @return 屏幕高px
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    /**
     * dip转为PX
     */
    public static int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }
}
