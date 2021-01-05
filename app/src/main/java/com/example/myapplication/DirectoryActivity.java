package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.CollationElementIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DirectoryActivity extends AppCompatActivity {
    ListView directoryList;
    List<String> directoryNames = new ArrayList<String>();
    final String rootPathDownload = String.valueOf(Environment.
            getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
    final String rootPath = rootPathDownload + "/WeedDetection";
    CustomAdapter customAdapter;
    File[] files;
    private static final String SHARED_PREFS = "storingFileData";
    String location = "empty";
    TextView f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.directories);

        // for storing dates
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        // Make sure rooth path exists, if not create it
        final File rootFolder = new File(rootPath);
        if (!rootFolder.exists()) {
            // add folder
            rootFolder.mkdir();
        }
        // set up list views
        directoryList = (ListView) findViewById(R.id.directoryListView);
        customAdapter = new CustomAdapter(getApplicationContext(), directoryNames);
        directoryList.setAdapter(customAdapter);
        updateDirectoryNames();
        customAdapter.notifyDataSetChanged();

        // add directory button
        FloatingActionButton add_directory = (FloatingActionButton) findViewById(R.id.add_directory);
        // allow user to create a directory
        add_directory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                final AlertDialog.Builder newFolderDialog =
                        new AlertDialog.Builder(DirectoryActivity.this);
                newFolderDialog.setTitle("New Folder:");
                final EditText folderName = new EditText(DirectoryActivity.this);
                folderName.setInputType(InputType.TYPE_CLASS_TEXT);
                newFolderDialog.setView(folderName);
                newFolderDialog.setPositiveButton("Create",
                        new DialogInterface.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newFolderName = rootPath + '/' + folderName.getText();
                                final File newFolder = new File(newFolderName);
                                if (!newFolder.exists()) {
                                    // add folder
                                    newFolder.mkdir();
                                    // get file data
                                    String currentTime = sdf.format(new Date());
                                    String currentDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(calendar.getTime());
                                    String currentDateandTime = currentDate + " at " + currentTime;
                                    String fileData = currentDateandTime + "\n" + location;
                                    saveFileData(newFolderName, fileData);
                                    // update contents of the list
                                    updateDirectoryNames();
                                    customAdapter.notifyDataSetChanged();
                                    Toast.makeText(DirectoryActivity.this, "Folder Created", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(DirectoryActivity.this, "Folder Already Exists", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                newFolderDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                newFolderDialog.show();
            }
        });

        // clicking on a directory
        directoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // pass root
                String passRootDirectory = customAdapter.getItem(position);
                Intent intent = new Intent(DirectoryActivity.this, FilesActivity.class);
                intent.putExtra("ROOT", passRootDirectory);
                startActivity(intent);
            }
        });
        // long clicking on directory
        directoryList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(DirectoryActivity.this);
                deleteDialog.setTitle("Delete");
                deleteDialog.setMessage("Do you really want to delete these files?");
                deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File toDelete = new File(customAdapter.getItem(position));
                        deleteFileOrFolder(toDelete);

                        Toast.makeText(DirectoryActivity.this, "Directory deleted", Toast.LENGTH_SHORT).show();
                        updateDirectoryNames();
                        customAdapter.notifyDataSetChanged();
                    }
                });
                deleteDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                deleteDialog.show();
                return true;
            }
        });

    }

    public void updateDirectoryNames() {
        File dir = new File(rootPath);
        files = dir.listFiles();
        int filesFoundCount;
        if (files != null) {
            filesFoundCount = files.length;
        } else {
            filesFoundCount = 0;
        }
        directoryNames.clear();
        for (int i=0; i<filesFoundCount; i++) {
            if (files[i].isDirectory()) {
                directoryNames.add(files[i].getAbsolutePath());
            }
        }
    }

    public class CustomAdapter extends BaseAdapter {
        Context context;
        List<String> directoryNames;
        LayoutInflater inflter;

        public CustomAdapter(Context applicationContext, List<String> directoryNames) {
            this.context = context;
            this.directoryNames = directoryNames;
            inflter = (LayoutInflater.from(applicationContext));
        }

        @Override
        public int getCount() {
            return directoryNames.size();
        }

        @Override
        public String getItem(int i) {
            return directoryNames.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflter.inflate(R.layout.directory_item, null);
            TextView folder_name = (TextView) view.findViewById(R.id.folder_name);
            TextView folder_specs = (TextView) view.findViewById(R.id.folder_specs);
            folder_name.setText(directoryNames.get(i).substring(directoryNames.get(i).lastIndexOf('/')+1));
            String specs = "Date Created: " + getFileData(directoryNames.get(i));
            folder_specs.setText(specs);
            return view;
        }
    }

    private void deleteFileOrFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteFileOrFolder(child);

        fileOrDirectory.delete();
    }

    private void saveFileData(String filename, String data) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // save date with filename as the key
        editor.putString(filename, data);
        editor.apply();
    }

    private String getFileData(String filename) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        return sharedPreferences.getString(filename, "Not Found");
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(DirectoryActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback() {

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(DirectoryActivity.this)
                                .removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0) {
                            int i = locationResult.getLocations().size() - 1;
                            double latitude =
                                    locationResult.getLocations().get(i).getLatitude();
                            double longitude =
                                    locationResult.getLocations().get(i).getLongitude();
                            location = "Latitude: " + latitude + " Longitude: " + longitude;
                        }
                    }

                }, Looper.getMainLooper());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
        startActivity(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDirectoryNames();
        customAdapter.notifyDataSetChanged();
    }
}
