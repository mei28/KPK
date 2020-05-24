package com.example.mezamashi;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.example.mezamashi.R;
import com.example.mezamashi.service.SoundService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

// 参考 https://github.com/hiroaki-dev/AlarmSample/blob/master/app/src/main/java/me/hiroaki/alarmsample/PlaySoundActivity.java

public class WakeUpActivity extends AppCompatActivity implements SensorEventListener {

    Button stopBtn;
    boolean shaked = false;

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;
    private long mShakeTimestamp;
    private int mShakeCount;
    private int SHAKE_MAX_COUNT = 30;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private TextView mComment, mLeftCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_wake_up);
        Toolbar toolbar = findViewById(R.id.toolbarWakeUp);
        setSupportActionBar(toolbar);


        startService(new Intent(this, SoundService.class));

        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(WakeUpActivity.this, SoundService.class));
            }
        });

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_UI);

        mLeftCount = (TextView) findViewById(R.id.left_count);
        mComment = (TextView) findViewById(R.id.comment);

        if (shaked == false) {
            stopBtn.setEnabled(false);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        // gForce will be close to 1 when there is no movement.
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();
            // ignore shake events too close to each other (500ms)
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }

            // reset the shake count after 3 seconds of no shakes
            if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                mShakeCount = 0;
            }

            mShakeTimestamp = now;
            mShakeCount++;
        }

//        Log.d("mezamashi", String.valueOf(mShakeCount));
        if (mShakeCount > SHAKE_MAX_COUNT && shaked == false) {
            shaked = true;
            stopBtn.setEnabled(true);
        }
        float progress = (float) (mShakeCount / SHAKE_MAX_COUNT);
        mLeftCount.setText("残り" + String.valueOf(Math.max(SHAKE_MAX_COUNT - mShakeCount,0)) + "回だよ！");
        if (mShakeCount == 0) {
            mComment.setText(String.valueOf(SHAKE_MAX_COUNT) + "回振って目覚ましを止めよう!");
        }else if(progress>=1.0) mComment.setText("おつかれさま，とめていいよ！");
            else if (progress > 0.8) mComment.setText("もうちょっとだよ，振って振って!");
        else if (progress > 0.5) mComment.setText("やっと半分，まだまだ振って!");
        else if (progress > 0.3) mComment.setText("振りはじめたばかりだね，がんばって！");
        else mComment.setText("アラームをとめるには振るしかないよ！");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}