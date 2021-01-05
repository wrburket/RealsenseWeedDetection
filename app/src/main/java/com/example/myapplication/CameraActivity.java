package com.example.myapplication;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.RsContext;

public class CameraActivity extends AppCompatActivity {
    // GlobalClass variable
    GlobalClass globalClass;
    // Spinner variables
    Spinner Depth_res;
    Spinner Depth_FPS;
    Spinner RGB_res;
    Spinner RGB_FPS;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.select_camera);

        // setting up all the spinners
        setupSpinners();

        globalClass = (GlobalClass) getApplicationContext();

        final Button configure_camera = (Button) findViewById(R.id.configure_camera);
        if (!globalClass.isCamera_connected()) {
            //configure_camera.setEnabled(false);
        }
        configure_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Depth_res.getSelectedItem()!=null) {
                    globalClass.setDepth_res(Depth_res.getSelectedItem().toString());
                    globalClass.setDept_res_pos(Depth_res.getSelectedItemPosition());
                }
                if (RGB_res.getSelectedItem()!=null) {
                    globalClass.setRGB_res(RGB_res.getSelectedItem().toString());
                    globalClass.setRGB_FPS_pos(RGB_res.getSelectedItemPosition());
                }
                if (Depth_FPS.getSelectedItem()!=null) {
                    globalClass.setDepth_FPS(Depth_FPS.getSelectedItem().toString());
                    globalClass.setDepth_FPS_pos(Depth_FPS.getSelectedItemPosition());
                }
                if (RGB_FPS.getSelectedItem()!=null) {
                    globalClass.setRGB_FPS(RGB_FPS.getSelectedItem().toString());
                    globalClass.setRGB_FPS_pos(RGB_FPS.getSelectedItemPosition());
                }
                Toast.makeText(CameraActivity.this, "Camera Configured", Toast.LENGTH_SHORT).show();
            }
        });

        //RsContext.init must be called once in the application's lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(getApplicationContext());


        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        RsContext mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(new DeviceListener() {
            @Override
            public void onDeviceAttach() {
                globalClass.setCamera_connected(true);
                configure_camera.setEnabled(true);
            }

            @Override
            public void onDeviceDetach() {
                globalClass.setCamera_connected(false);
                configure_camera.setEnabled(false);
            }
        });
    }


    // Checks if all permissions have been granted
    private void setupSpinners() {
        //Depth Resoltuion
        Depth_res = (Spinner) findViewById(R.id.Depth_res);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.depth_res, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Depth_res.setAdapter(adapter);
        //Depth FPS
        Depth_FPS = (Spinner) findViewById(R.id.Depth_FPS);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this,
                R.array.FPS, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Depth_FPS.setAdapter(adapter1);

        //RGB Resoltuion
        RGB_res = (Spinner) findViewById(R.id.RGB_res);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.RGB_res, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        RGB_res.setAdapter(adapter2);
        //RGB FPS
        RGB_FPS = (Spinner) findViewById(R.id.RGB_FPS);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this,
                R.array.FPS, android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        RGB_FPS.setAdapter(adapter3);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Depth_res.setSelection(globalClass.getDept_res_pos());
        Depth_FPS.setSelection(globalClass.getDepth_FPS_pos());
        RGB_FPS.setSelection(globalClass.getRGB_FPS_pos());
        RGB_res.setSelection(globalClass.getRGB_res_pos());

    }
}