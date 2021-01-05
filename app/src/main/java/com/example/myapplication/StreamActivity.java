package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.intel.realsense.librealsense.Colorizer;
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


public class StreamActivity extends AppCompatActivity {

    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;

    private boolean mPermissionsGranted = false;

    private Context mAppContext;
    private TextView mBackGroundText;
    private GLRsSurfaceView mGLSurfaceView;
    private boolean mIsStreaming = false;
    private final Handler mHandler = new Handler();

    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private RsContext mRsContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);            //set layout

        mAppContext = getApplicationContext();              //get app context
        mBackGroundText = findViewById(R.id.connectCameraText);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);          //surfaceview used for openGL rendering
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA); // request permissions if not already granted
            return;
        }

        mPermissionsGranted = true;             //permissions granted

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLSurfaceView.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mPermissionsGranted)
            init();                 //initialize
        else
            Log.e(TAG, "missing permissions");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRsContext != null)
            mRsContext.close();
        stop();
        mColorizer.close();
        mPipeline.close();
    }

    private void init(){
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.

        RsContext.init(mAppContext);

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();             // new pipeline
        mColorizer = new Colorizer();           // declare depth colorizer

        try(DeviceList dl = mRsContext.queryDevices()){         //check context for device
            if(dl.getDeviceCount() > 0) {                   //device count
                showConnectLabel(false);
                start();                                //run start
            }
        }
    }

    private void showConnectLabel(final boolean state){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBackGroundText.setVisibility(state ? View.VISIBLE : View.GONE);    //shows (connect to camera text)
            }
        });
    }

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
            showConnectLabel(false);
        }

        @Override
        public void onDeviceDetach() {
            showConnectLabel(true);
            stop();
        }
    };

    Runnable mStreaming = new Runnable() {                  //Streaming
        @Override
        public void run() {
            try {
                try(FrameSet frames = mPipeline.waitForFrames()) {              // waiting for frames
                    try(FrameSet processed = frames.applyFilter(mColorizer)) {          //process
                        mGLSurfaceView.upload(processed);       //upload colorized depth image
                    }
                }
                mHandler.post(mStreaming);
            }
            catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private void configAndStart() throws Exception {
        try(Config config  = new Config())
        {
            config.enableStream(StreamType.DEPTH, 640, 480, StreamFormat.Z16);    //enable depth stream
            config.enableStream(StreamType.COLOR, 640, 480, StreamFormat.RGB8);    //enable color stream
            // try statement needed here to release resources allocated by the Pipeline:start() method
            try(PipelineProfile pp = mPipeline.start(config)){}
        }
    }

    private synchronized void start() {
        if(mIsStreaming)
            return;
        try{
            Log.d(TAG, "try start streaming");
            mGLSurfaceView.clear();         //clear surface
            configAndStart();               //enable streams
            mIsStreaming = true;
            mHandler.post(mStreaming);          //schedule streaming
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to start streaming");
        }
    }

    private synchronized void stop() {
        if(!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();
            mGLSurfaceView.clear();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            Log.d(TAG, "failed to stop streaming");
        }
    }
}