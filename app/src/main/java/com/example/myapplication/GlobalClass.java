package com.example.myapplication;

import android.app.Application;
import android.widget.Toast;

import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import java.net.URI;

public class GlobalClass extends Application {
    private String depth_res;
    private String RGB_res;
    private String Depth_FPS;
    private String RGB_FPS;
    private int dept_res_pos;
    private int depth_FPS_pos;
    private int RGB_res_pos;
    private int RGB_FPS_pos;
    private boolean camera_connected;
    // for setting up azure
    public String sasToken;
    public StorageUri uriStorage;
    public CloudBlobContainer azureContainer;

    public int getDept_res_pos() {
        return dept_res_pos;
    }

    public void setDept_res_pos(int dept_res_ps) {
        this.dept_res_pos = dept_res_ps;
    }

    public int getDepth_FPS_pos() {
        return depth_FPS_pos;
    }

    public void setDepth_FPS_pos(int depth_FPS_pos) {
        this.depth_FPS_pos = depth_FPS_pos;
    }

    public int getRGB_res_pos() {
        return RGB_res_pos;
    }

    public void setRGB_res_pos(int RGB_res_pos) {
        this.RGB_res_pos = RGB_res_pos;
    }

    public int getRGB_FPS_pos() {
        return RGB_FPS_pos;
    }

    public void setRGB_FPS_pos(int RGB_FPS_pos) {
        this.RGB_FPS_pos = RGB_FPS_pos;
    }

    public String getDepth_FPS() {
        if (Depth_FPS==null) return "30";
        return Depth_FPS;
    }

    public void setDepth_FPS(String depth_FPS) {
        Depth_FPS = depth_FPS;
    }

    public String getRGB_FPS() {
        if (RGB_FPS==null) return "30";
        return RGB_FPS;
    }

    public void setRGB_FPS(String RGB_FPS) {
        this.RGB_FPS = RGB_FPS;
    }

    public boolean isCamera_connected() {
        return camera_connected;
    }

    public void setCamera_connected(boolean camera_connected) {
        this.camera_connected = camera_connected;
    }

    public String getDepth_res() {
        if (depth_res==null) return "640x480";
        return depth_res;
    }

    public void setDepth_res(String depth_res) {
        this.depth_res = depth_res;
    }

    public String getRGB_res() {
        if (depth_res==null) return "1280x780";
        return RGB_res;
    }

    public void setRGB_res(String rgb_res) {
        this.RGB_res = rgb_res;
    }

}
