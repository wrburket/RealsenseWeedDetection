package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;

import java.io.File;

public class Playback extends AppCompatActivity {
    private static final String TAG = "librs playback example";
    private static final int READ_REQUEST_CODE = 1;
    private Uri mUri;

    private boolean mPermissionsGranted = true;

    private GLRsSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);                 // set view to playback activity
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        init();                                                 // start streaming
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.close();                     // close surface view on Destroy
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return;
        }
        mPermissionsGranted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mUri == null){
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        } else{
            init();                     // start streaming
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStreaming.interrupt();                                 // interrupt streaming while paused
        if(mStreaming.isAlive()) {
            try {
                mStreaming.join(1000);
                mGLSurfaceView.clear();                     // clear the surface view
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void init(){
        mStreaming.start();                     // start streaming
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                mUri = data.getData();
            }
        }
    }

    Thread mStreaming = new Thread() {
        @Override
        public void run() {
            // file path is the absolute path for testing and must be altered

            String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/WeedDetection/beta/20201105_112126.bag" ;
            try(Colorizer colorizer = new Colorizer()) {                // create new colorizer
                try (Config config = new Config()) {                    // configure stream

                    config.enableDeviceFromFile(filePath);              // enable reading from file

                    try (Pipeline pipeline = new Pipeline()) {          // create pipeline
                        try {


                            try (PipelineProfile pp = pipeline.start(config)) {}                // start pipeline

                            while (!mStreaming.isInterrupted()) {                       // while streaming:
                                try (FrameSet frames = pipeline.waitForFrames()) {                      // wait for frames to come in

                                    try (FrameSet processed = frames.applyFilter(colorizer)) {              // apply colorizer to depth frames
                                        mGLSurfaceView.upload(processed);                            // upload colorized frame
                                    }
                                }
                            }
                            pipeline.stop();                                // close the pipeline
                        } catch (Exception e) {
                            Log.e(TAG, "streaming, error: " + e.getMessage());
                        }
                    }
                }
            }
        }
    };
}