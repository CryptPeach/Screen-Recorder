package com.app.libder.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.app.libder.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recorder extends AppCompatActivity {
    private static final String TAG = "ScreenRecorderActivity";

    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private ToggleButton mToggleButton;
    private boolean isRecording = false;
    private Intent serviceIntent;
    private int resultCode;
    private Intent data;
    private MediaProjectionCallback mMediaProjectionCallback;

    private int DISPLAY_WIDTH;
    private int DISPLAY_HEIGHT;

    private final ActivityResultLauncher<Intent> screenCaptureResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            resultCode = result.getResultCode();
                            data = result.getData();
                            startRecordingService();
                        } else {
                            Toast.makeText(this, "Screen recording permission denied",
                                    Toast.LENGTH_SHORT).show();
                            mToggleButton.setChecked(false);
                        }
                    });

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                stopScreenRecording();
                mToggleButton.setChecked(false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aa);

        setupScreenMetrics();

        mToggleButton = findViewById(R.id.rewind_buttonzz);
        mToggleButton.setOnClickListener(this::onToggleScreenShare);

        mProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        serviceIntent = new Intent(this, com.app.libder.Activity.ScreenRecordingService.class);
        mMediaProjectionCallback = new MediaProjectionCallback();
    }

    private void setupScreenMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        DISPLAY_WIDTH = metrics.widthPixels;
        DISPLAY_HEIGHT = metrics.heightPixels;

        float scale = 0.5f;
        DISPLAY_WIDTH = (int) (DISPLAY_WIDTH * scale);
        DISPLAY_HEIGHT = (int) (DISPLAY_HEIGHT * scale);
    }

    @SuppressLint("NewApi")
    private void startRecordingService() {
        startForegroundService(serviceIntent);
        new android.os.Handler().postDelayed(() -> {
            startScreenRecording();
        }, 100);
    }

    private void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            startScreenCapture();
        } else {
            stopScreenRecording();
        }
    }

    private void startScreenCapture() {
        if (mProjectionManager != null) {
            screenCaptureResultLauncher.launch(mProjectionManager.createScreenCaptureIntent());
        }
    }

    private void startScreenRecording() {
        try {
            initRecorder();
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (mMediaProjection != null) {
                // Register callback before starting capture
                mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                mVirtualDisplay = createVirtualDisplay();
                mMediaRecorder.start();
                isRecording = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen recording: " + e.getMessage());
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            stopService(serviceIntent);
        }
    }

    private void stopScreenRecording() {
        if (isRecording) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording: " + e.getMessage());
            }

            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }

            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mMediaProjectionCallback);
                mMediaProjection.stop();
                mMediaProjection = null;
            }

            isRecording = false;
            stopService(serviceIntent);
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenRecorder",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);
    }

    private String getOutputFilePath() {
        File mediaDir = new File(getApplicationContext().getFilesDir(), "recordings");
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        String fileName = "screen_record_" + timestamp + ".mp4";

        return new File(mediaDir, fileName).getAbsolutePath();
    }

    private void initRecorder() {
        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mMediaRecorder.setOutputFile(getOutputFilePath());

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaRecorder preparation failed: " + e.getMessage());
            Toast.makeText(this, "Failed to prepare recorder", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScreenRecording();
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
}