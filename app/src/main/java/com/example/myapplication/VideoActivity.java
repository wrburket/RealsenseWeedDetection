package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoActivity extends AppCompatActivity {
    // GlobalClass variable
    GlobalClass globalClass;

    private static final String TAG = "librs recording example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int PERMISSIONS_REQUEST_WRITE = 1;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
    private GLRsSurfaceView mGLSurfaceView;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private RsContext mRsContext;

    private FloatingActionButton mStartRecordFab;
    private FloatingActionButton mStopRecordFab;

    String rootPath;
    int clicked;

    int i;
    // resolution variables
    int depth_height, depth_width, RGB_height, RGB_width;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // get camera settings
        globalClass = (GlobalClass) getApplicationContext();
        // get depth resolution settings
        String depth_res = globalClass.getDepth_res();
        String[] depth_tokens = depth_res.split("x");
        depth_width = Integer.parseInt(depth_tokens[0]);
        depth_height = Integer.parseInt(depth_tokens[1]);
        // get RGB resolution settings
        String RGB_res = globalClass.getRGB_res();
        String[] RGB_tokens = RGB_res.split("x");
        RGB_width = Integer.parseInt(RGB_tokens[0]);
        RGB_height = Integer.parseInt(RGB_tokens[1]);
        Toast.makeText(VideoActivity.this, Integer.toString(RGB_width), Toast.LENGTH_SHORT).show();


        rootPath = getIntent().getStringExtra("ROOT");                  // file path for saving in internal storage

        mAppContext = getApplicationContext();
        mBackGroundText = findViewById(R.id.connectCameraText);                 // prompts user to connect a device
        mGLSurfaceView = findViewById(R.id.glSurfaceView);                      // camera surface view

        mStartRecordFab = findViewById(R.id.startRecordFab);                    // "start recording" floating action button
        mStopRecordFab = findViewById(R.id.stopRecordFab);                      // "stop recording" floating action button

        mStartRecordFab.setOnClickListener(new View.OnClickListener() {         // button to start recording
            @Override
            public void onClick(View view) {
                toggleRecording();                      // toggle recording on click
                clicked = 1;                            // sets "clicked" variable
            }
        });
        mStopRecordFab.setOnClickListener(new View.OnClickListener() {          // button to stop recording
            @Override
            public void onClick(View view) {
                toggleRecording();                              // toggle recording
                clicked = 0;                                    // resets "clicked" variable to zero
            }
        });

        // check for permission to write to external storage

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        mPermissionsGranted = true;         // permissions are granted
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.close();             // close surface view
    }

    @Override
    // checks permissions to write files
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_WRITE);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }


        mPermissionsGranted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mPermissionsGranted)                 // if permissions are given:
            init();                             // initialize pipeline
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRsContext != null)
            mRsContext.close();         // close context
        stop();                         // stop streaming
    }

    private String getFilePath(){
        // line commented out below saves bag files to "rs_bags" folder in external storage instead (used for testing)

        // File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "rs_bags");


        File folder = new File(rootPath);                       // saves in app internal storage
        folder.mkdir();                                         // create new file
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");         // date and time info
        String currentDateAndTime = sdf.format(new Date());                             // save in simple date format
        File file = new File(folder, currentDateAndTime + ".bag");              // save new .bag file
        return file.getAbsolutePath();                                          // returns file path

    }

    void init(){

        RsContext.init(mAppContext);        // get app context


        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();         // establish pipeline

        try(DeviceList dl = mRsContext.queryDevices()){
            if(dl.getDeviceCount() > 0) {               // check the device count
                showConnectLabel(false);         // hide label prompting user to connect device
                start(false);                   // start streaming but not recording
            }
        }
    }

    private void showConnectLabel(final boolean state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);        // show label if device is not connected

                if(state){                                  // if "connect label" is showing:
                    mStartRecordFab.hide();                 // hide recording button
                }
                else {
                    mStartRecordFab.show();                // show recording button
                }
                mStopRecordFab.hide();

            }
        });
    }

    private void toggleRecording(){


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // toggles recording
                if(mStartRecordFab.getVisibility() == View.GONE){
                    mStartRecordFab.show();                         // show recording button
                    stop();                                         // stop recording
                    Toast.makeText(VideoActivity.this, "saved", Toast.LENGTH_SHORT).show();     // alerts if file's been saved
                }
                else {
                    mStartRecordFab.hide();
                    start(true);                            // start recording
                }
                if(mStopRecordFab.getVisibility() == View.GONE){
                    mStopRecordFab.show();                          // show the "stop recording" button
                }
                else {
                    mStopRecordFab.hide();                          // hide the "stop recording" button

                }

            }
        });
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {              // when device is attached
            showConnectLabel(false);         // camera is connected, message goes away
        }

        @Override
        public void onDeviceDetach() {              // if device is detached
            showConnectLabel(true);         // tells user to connect camera
            stop();                               // stop streaming
        }
    };

    Runnable mStreaming = new Runnable() {
        @Override
        public void run() {
            try {
                try(FrameSet frames = mPipeline.waitForFrames()) {          // wait for frames to come in
                    mGLSurfaceView.upload(frames);                  // render frames to device's screen
                }
                mHandler.post(mStreaming);
            }
            catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private synchronized void start(boolean record) {
        if(mIsStreaming)
            return;
        try{
            mGLSurfaceView.clear();
            Log.d(TAG, "try start streaming");
            try(Config cfg = new Config()) {
                // enable both depth and RGB stream, set resolution and frame rate
                cfg.enableStream(StreamType.DEPTH, 0, depth_width, depth_height, StreamFormat.Z16, Integer.parseInt(globalClass.getDepth_FPS()));
                cfg.enableStream(StreamType.COLOR, 0, RGB_width, RGB_height, StreamFormat.RGB8, Integer.parseInt(globalClass.getRGB_FPS()));
                if (record)
                    i = 1;
                cfg.enableRecordToFile(getFilePath());          // enable writing to file



                try(PipelineProfile pp = mPipeline.start(cfg)){             // start pipeline

                }

            }
            mIsStreaming = true;
            mHandler.post(mStreaming);
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    private synchronized void stop() {
        if(!mIsStreaming)
            return;
        try {                                           // end streaming
            i = 0;
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();                           // close pipeline
            mGLSurfaceView.clear();                             // clear surface view
            Log.d(TAG, "streaming stopped successfully");
        }  catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
            mPipeline = null;
        }
    }
}