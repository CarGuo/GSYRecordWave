package com.shuyu.app;


import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.PermissionUtils;

/**
 * Created by shuyu on 2016/11/15.
 * 声音波形，录制与播放
 */
public class MainActivity extends AppCompatActivity {


    @BindView(R.id.main_frameLayout)
    FrameLayout mainFrameLayout;


    MainFragment newFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        newFragment = new MainFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_frameLayout, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        // Request permissions based on API level
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permissions = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6+ (API 23+)
            permissions = new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            // Below API 23, permissions are granted at install time
            return;
        }

        boolean hasAllPermissions = PermissionUtils.hasSelfPermissions(this, permissions);
        if (!hasAllPermissions) {
            requestPermissions(permissions, 1110);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionResult = PermissionUtils.verifyPermissions(grantResults);
        if (!permissionResult) {
            Toast.makeText(this, "没获取到存储和录音权限，无法正常运行", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onBackPressed() {
        if(newFragment.onBackPress()) {
            return;
        }
        finish();
    }
}
