package com.example.tfai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    FrameLayout fragmentHolder;
    HomeFragment home;
    NavigationView navdrawer;
    ImageView menuicon;
    DrawerLayout drawerLayout;
    FragmentTransaction ft;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
    };

    private static final int PERMISSION_REQUEST_CODE = 1001;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndStartService();

        fragmentHolder = findViewById(R.id.frag_content);
        navdrawer = findViewById(R.id.nav_drawer);
        menuicon = findViewById(R.id.menu_icon);
        drawerLayout = findViewById(R.id.drawer_layout);
        home = new HomeFragment();
        ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.frag_content, home);
        ft.addToBackStack(null);
        ft.commit();

        //OnclickListeners
        menuicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        navdrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                Log.d("Navbar", item.getItemId() + "Click");
                switch (item.getItemId()){

                    case R.id.item_contact:
                        FragmentTransaction fxt= getSupportFragmentManager().beginTransaction();
                        fxt.replace(R.id.frag_content,new ContactFragment());
                        fxt.commit();
                        break;
                    case R.id.item_help:
                        FragmentTransaction fyt= getSupportFragmentManager().beginTransaction();
                        fyt.replace(R.id.frag_content,new HelpFragment());
                        fyt.commit();
                        break;
                    case R.id.item_mobile_contact:
                        FragmentTransaction fxt1= getSupportFragmentManager().beginTransaction();
                        fxt1.replace(R.id.frag_content,new MobileContactFragment());
                        fxt1.commit();
                        break;
                    case R.id.item_feedback:
                        Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
                        startActivity(intent);
                        break;
                    default:
                        FragmentTransaction fzt= getSupportFragmentManager().beginTransaction();
                        fzt.replace(R.id.frag_content,new HomeFragment());
                        fzt.commit();

                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("final_mod_opt.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    private boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length >=8) {
                startYourService();
            } else {
                // Handle the case where permissions are not granted
                Toast.makeText(this, "Permissions are required for this app to function.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void startYourService() {
        try {
            Interpreter interpreter = new Interpreter(loadModelFile(this));
            Log.d("LOAD", "LOADED SUCCESSFULLY");

            // Start your service if all permissions are granted
            Intent serviceIntent = new Intent(this, SpeechRecognitionService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("LOAD", e.getMessage());
        }
    }
    private void checkAndStartService() {
        if (hasAllPermissions(this)) {
            if (!isServiceRunning(SpeechRecognitionService.class)) {
                startYourService();
            } else {
                Log.d("LOAD", "Service is already running.");
            }
        } else {
            requestPermissions();
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }



}