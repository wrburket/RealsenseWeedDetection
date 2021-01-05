package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.RsContext;
import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.core.PathUtility;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };
    private static final int PERMISSIONS_COUNT = PERMISSIONS.length;
    TextView textViewLocation;

    private RsContext mRsContext;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private String token;
    // GlobalClass variable
    GlobalClass globalClass;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // get date and time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String currentDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(calendar.getTime());
        String currentDateandTime = currentDate + "\nTime: " + currentTime;
        // display date and time
        TextView textViewDate = findViewById(R.id.date);
        textViewDate.setText(currentDateandTime);

        // global class
        globalClass = (GlobalClass) getApplicationContext();
        globalClass.setCamera_connected(false);
        globalClass.sasToken = "?sv=2019-10-10&ss=bfqt&srt=sco&sp=rwdlacup&se=2020-11-06T00:34:20Z&st=2020-11-05T16:34:20Z&spr=https&sig=z%2FukgzxmHuHsQJaruoil0myiLyZT8vVMkbJr%2FFJgYjY%3D";
        globalClass.uriStorage = new StorageUri(URI.create("https://weedsmedia.blob.core.usgovcloudapi.net/"));
        globalClass.azureContainer = null;

        // azure setup
        //AzureActivityKt.setupAzure(globalClass.sasToken, globalClass.uriStorage);
        setupAzure();

        // getting location
        // checking permissions for location and storage first
        textViewLocation = findViewById(R.id.location);
        if (!arePermissionsGranted()) {
            // request permissions if needed
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS,
                    REQUEST_PERMISSIONS
            );
        }
        else {
            getCurrentLocation();
        }



        // for navigating to FileAcvitvity
        Button toCameraSelect = (Button) findViewById(R.id.toCameraSelect);
        toCameraSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        });

        Button toFilePage = (Button) findViewById(R.id.toFilePage);
        toFilePage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DirectoryActivity.class));
            }
        });

        //RsContext.init must be called once in the application's lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(getApplicationContext());

        printMessage();

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(new DeviceListener() {
            @Override
            public void onDeviceAttach() {
                globalClass.setCamera_connected(true);
                printMessage();
            }

            @Override
            public void onDeviceDetach() {
                globalClass.setCamera_connected(false);
                printMessage();
            }
        });
    }

    // Checks if all permissions have been granted
    private boolean arePermissionsGranted(){
        int p = 0;
        while (p<PERMISSIONS_COUNT) {
            if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            p++;
        }
        return true;
    }

    // Closes application if permissions have not been granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_PERMISSIONS && grantResults.length>0){
            if (arePermissionsGranted()) {
                getCurrentLocation();
            }  else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
        }
    }

    // Gets current location
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                            .removeLocationUpdates(this);
                    if (locationResult != null && locationResult.getLocations().size() > 0) {
                        int i = locationResult.getLocations().size() - 1;
                        double latitude =
                            locationResult.getLocations().get(i).getLatitude();
                        double longitude =
                            locationResult.getLocations().get(i).getLongitude();
                        String currentLocation = String.format("Latitude: %s\nLongitude: %s",
                            latitude,
                            longitude);
                        textViewLocation.setText(currentLocation);
                    }
                }

        }, Looper.getMainLooper());
    }

    private void printMessage(){
        // Example of a call to native methods
        int cameraCount = nGetCamerasCountFromJNI();
        final String version = nGetLibrealsenseVersionFromJNI();
        final String cameraCountString;
        if(cameraCount == 0)
            cameraCountString = "No cameras are currently connected.";
        else
            cameraCountString = "Camera is connected";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView camera_connected = (TextView) findViewById(R.id.camera_connected);
                camera_connected.setText("This app use librealsense: " + version + "\n" + cameraCountString);
            }
        });
    }

    //****************************************************************************************************************
    // azure functions
    //****************************************************************************************************************
    // set up azure storage
    public void setupAzure() {
        try {
            StorageCredentialsSharedAccessSignature accountSAS = new StorageCredentialsSharedAccessSignature(globalClass.sasToken);
            CloudBlobClient blobClient = new CloudBlobClient(globalClass.uriStorage, accountSAS);
            globalClass.azureContainer = blobClient.getContainerReference("intel-images");
            Toast.makeText(this, "Azure setup successful", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Log.v("LXT", "other");
            Toast.makeText(this, "Azure setup failed", Toast.LENGTH_SHORT).show();
        }
    }


    // refresh the page when using the back button
    @Override
    public void onRestart()
    {
        super.onRestart();
        finish();
        startActivity(getIntent());
    }

    private static native String nGetLibrealsenseVersionFromJNI();
    private static native int nGetCamerasCountFromJNI();
}
