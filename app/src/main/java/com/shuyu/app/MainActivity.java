package com.shuyu.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.shuyu.waveview.AudioWaveView;
import com.shuyu.waveview.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

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
    @BindView(R.id.activity_main)
    RelativeLayout activityMain;


    MP3Recorder mRecorder;

    String filePath;

    boolean mIsRecord = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        resolveNormalUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRecord) {
            resolveStopRecord();
        }
    }

    @OnClick({R.id.record, R.id.stop, R.id.play, R.id.reset})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record:
                resolveRecord();
                break;
            case R.id.stop:
                resolveStopRecord();
                break;
            case R.id.play:
                break;
            case R.id.reset:
                break;
        }
    }


    private void resolveNormalUI() {
        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveRecordUI() {
        record.setEnabled(false);
        stop.setEnabled(true);
        play.setEnabled(false);
        reset.setEnabled(false);
    }

    private void resolveStopUI() {
        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolvePlayUI() {
        record.setEnabled(false);
        stop.setEnabled(false);
        play.setEnabled(true);
        reset.setEnabled(true);
    }

    private void resolveRecord() {
        filePath = FileUtils.getAppPath();
        File file = new File(filePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        mRecorder = new MP3Recorder(new File(filePath));
        int size = getScreenWidth(this) / dip2px(this, 1);//控件默认的间隔是1
        mRecorder.setDataList(audioWave.getRecList(), size);
        mRecorder.setErrorHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(MainActivity.this, "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    resolveError();
                }
            }
        });

        try {
            mRecorder.start();
            audioWave.startView();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "录音出现异常", Toast.LENGTH_SHORT).show();
            resolveError();
            return;
        }
        resolveRecordUI();
        mIsRecord = true;
    }

    private void resolveStopRecord() {
        resolveStopUI();
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWave.stopView();
        }
        mIsRecord = false;

    }

    private void resolveError() {
        resolveNormalUI();
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
            audioWave.stopView();
        }
    }

    private void resolvePlayRecord() {
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void resolveResetPlay() {
        filePath = "";
    }


    /**
     * 获取屏幕的宽度px
     *
     * @param context 上下文
     * @return 屏幕宽px
     */
    private int getScreenWidth(Context context) {
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
    private int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    /**
     * dip转为PX
     */
    private int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }


}
