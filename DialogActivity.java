package com.example.tfai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class DialogActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private int photoCount = 0;
    private final int MAX_PHOTOS = 5;
    DBHelper db;
    DBHelper db1;
    private final ArrayList<File> capturedImages = new ArrayList<>();
    private final int maxImages = 5; // Number of images to capture

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activitt);
        db=new DBHelper(this,"ContactList");
        db1 = new DBHelper(this,"MobileContactList");
        startBackgroundThread();
        openCamera();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, MAX_PHOTOS);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 200);
                return;
            }
            cameraManager.openCamera(cameraId, cameraDeviceCallback, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            takePicture();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void takePicture() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    capturePhoto();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(DialogActivity.this, "Failed to configure camera", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void capturePhoto() {
        if (photoCount < MAX_PHOTOS) {
            try {
                // Get the device rotation
                int deviceRotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getRotation();

                // Map the device rotation to an orientation angle for the capture request
                int rotation = ORIENTATIONS.get(deviceRotation);

                // Set the orientation in the capture request to correct the image
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

                // Capture the photo with the correct orientation
                cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, backgroundHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            closeCamera();
        }
    }



    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Integer flashMode = captureRequestBuilder.get(CaptureRequest.FLASH_MODE);
            // Ensure flash is on before capturing
            if (flashMode != null && flashMode == CaptureRequest.FLASH_MODE_TORCH) {
                photoCount++;
                capturePhoto();
            } else {
                // Flash is off, handle accordingly (e.g., show a message)
                Log.d("CapturePhoto", "Flash is off, photo capture skipped.");
            }
        }
    };

    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (capturedImages.size() >= maxImages) {
                // Stop capturing if max number of images is reached
                sendImagesViaEmail();
                sendTextMessage();
                return;
            }

            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                // Save the image bytes to a temp file
                File tempFile = saveImageToTempFile(bytes);
                if (tempFile != null) {
                    capturedImages.add(tempFile);
                    Log.d("ImageSaved", "Image saved to temp file: " + tempFile.getAbsolutePath());
                }

                // Check if 5 images have been captured and send email
                if (capturedImages.size() >= maxImages) {
                    sendImagesViaEmail();
                    sendTextMessage();
                }
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }

        private File saveImageToTempFile(byte[] bytes) {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("IMG_", ".jpg", getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(bytes);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return tempFile;
        }
    };


    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }


        stopBackgroundThread();
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void blinkFlash()
    {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String myString = "010101";
        long blinkDelay = 500; //Delay in ms
        for (int i = 0; i < myString.length(); i++) {
            if (myString.charAt(i) == '0') {
                try {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, true);
                } catch (CameraAccessException e) {

                }
            } else {
                try {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, false);
                } catch (CameraAccessException e) {
                }
            }
            break;
        }
    }

    private void sendImagesViaEmail() {
        // Ensure you have permission to access location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get the current location
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Create a Google Maps link for the current location
                        String locationLink = "https://www.google.com/maps/@?api=1&map_action=map&center=" + latitude + "," + longitude + "&zoom=14&basemap=roadmap";
                        String helpMessage= "";
                        Intent intent = getIntent();
                        if (intent != null && intent.hasExtra("emergency_message")) {
                            helpMessage = intent.getStringExtra("emergency_message") + locationLink;
                        }else {
                            helpMessage = "Hey, I am not feeling safe here. Please respond ASAP. My current location: " + locationLink;
                        }

                        ArrayList<Uri> imageUris = new ArrayList<>();
                        for (File file : capturedImages) {
                            Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
                            imageUris.add(uri);
                        }

                        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        emailIntent.setType("application/octet-stream"); // Set MIME type
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Alert");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, helpMessage);

                        ArrayList<String> contacts = db.getAllContacts();
                        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Get current user's email
                        triggerSOS(contacts);
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, contacts.toArray(new String[0]));

                        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);

                        // Grant URI permissions to email app
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Check if the email app is available
                        if (emailIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(emailIntent);
                        } else {
                            Toast.makeText(this, "Email app not installed.", Toast.LENGTH_SHORT).show();
                        }

                        // Clear the list after sending the email
                        capturedImages.clear();
                    } else {
                        Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendTextMessage() {
        // Ensure you have permission to access location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get the current location
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Create a Google Maps link for the current location
                        String locationLink = "https://www.google.com/maps/@?api=1&map_action=map&center=" + latitude + "," + longitude + "&zoom=14&basemap=roadmap";

                        // Create the message
                        String helpMessage;
                        Intent intent = getIntent();
                        if (intent != null && intent.hasExtra("emergency_message")) {
                            helpMessage = intent.getStringExtra("emergency_message") + " " + locationLink;
                        } else {
                            helpMessage = "Hey, I am not feeling safe here. Please respond ASAP. My current location: " + locationLink;
                        }

                        // Send SMS to all emergency contacts
                        ArrayList<String> contacts = db1.getAllContacts();
                        SmsManager smsManager = SmsManager.getDefault();
                        for (String contact : contacts) {
                            smsManager.sendTextMessage(contact, null, helpMessage, null, null);
                        }

                        Toast.makeText(this, "Message sent to emergency contacts.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, call sendImagesViaEmail() again
                sendImagesViaEmail();
                sendTextMessage();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void triggerSOS(ArrayList<String> contacts) {

        DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("sos_triggers");
        for (String userEmail: contacts) {
            // Create a new SOS trigger entry
            String triggerId = sosRef.push().getKey();

            if (triggerId != null) {
                SOS sos = new SOS(userEmail, ServerValue.TIMESTAMP);
                sosRef.child(triggerId).setValue(sos)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("SOS_TRIGGER", "SOS trigger updated successfully.");
                            } else {
                                Log.e("SOS_TRIGGER", "Failed to update SOS trigger: " + task.getException().getMessage());
                            }
                        });
            }
        }
    }


}

