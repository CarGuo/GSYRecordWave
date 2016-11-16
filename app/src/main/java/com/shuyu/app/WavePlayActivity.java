package com.shuyu.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RelativeLayout;

import com.piterwilson.audio.MP3RadioStreamDelegate;
import com.piterwilson.audio.MP3RadioStreamPlayer;
import com.shuyu.waveview.AudioWaveView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.shuyu.app.MainActivity.dip2px;
import static com.shuyu.app.MainActivity.getScreenWidth;

public class WavePlayActivity extends AppCompatActivity implements MP3RadioStreamDelegate {

    private final static String TAG = "WavePlayActivity";

    @BindView(R.id.audioWave)
    AudioWaveView audioWave;
    @BindView(R.id.activity_wave_play)
    RelativeLayout activityWavePlay;


    MP3RadioStreamPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave_play);
        ButterKnife.bind(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                play();
            }
        }, 1000);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioWave.stopView();
        stop();
    }

    private void play() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        player = new MP3RadioStreamPlayer();
        player.setUrlString(getIntent().getStringExtra("uri"));
        player.setDelegate(this);

        int size = getScreenWidth(this) / dip2px(this, 1);//控件默认的间隔是1
        player.setDataList(audioWave.getRecList(), size);
        audioWave.startView();
        try {
            player.play();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void stop() {
        player.stop();
    }


    /****************************************
     * Delegate methods. These are all fired from a background thread so we have to call any GUI code on the main thread.
     ****************************************/

    @Override
    public void onRadioPlayerPlaybackStarted(MP3RadioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerPlaybackStarted");
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
            }
        });
    }

    @Override
    public void onRadioPlayerStopped(MP3RadioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerStopped");
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
            }
        });

    }

    @Override
    public void onRadioPlayerError(MP3RadioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerError");
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
            }
        });

    }

    @Override
    public void onRadioPlayerBuffering(MP3RadioStreamPlayer player) {
        Log.i(TAG, "onRadioPlayerBuffering");
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
            }
        });

    }

}
