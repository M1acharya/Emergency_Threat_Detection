package com.example.tfai;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SpeechRecognitionService extends Service {
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private DatabaseReference sosReference;
    private ValueEventListener sosListener;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSpeechRecognizer();
        sosReference = FirebaseDatabase.getInstance().getReference("sos_triggers");

        // Set up a listener for SOS triggers
        sosListener = sosReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot sosSnapshot : snapshot.getChildren()) {
                    String userEmail = sosSnapshot.child("user_email").getValue(String.class);
                    String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                    // Check if the current user is an emergency contact
                    if (userEmail != null && userEmail.equals(currentUserEmail)) {
                        handleEmergency(sosSnapshot);

                        // Delete the SOS trigger entry
                        String sosKey = sosSnapshot.getKey();
                        if (sosKey != null) {
                            sosReference.child(sosKey).removeValue()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Log.d("SOS_TRIGGER", "SOS entry deleted successfully for: " + userEmail);
                                        } else {
                                            Log.e("SOS_TRIGGER", "Failed to delete SOS entry for " + userEmail + ": " + task.getException().getMessage());
                                        }
                                    });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                // Restart listening if there's an error
                startListening();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        if (match.equalsIgnoreCase("help me") ||
                                match.equalsIgnoreCase("save me") ||
                                match.equalsIgnoreCase("emergency")) {
                            handleVoiceCommand();
                            break;
                        }
                    }
                }
                startListening();
            }


            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        startListening();
    }

    private void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void handleVoiceCommand() {
        if (isAppInForeground()) {
            // App is in the foreground, directly open the activity
            openDialogActivity();
        } else {
            // App is in the background, show a notification to prompt the user
            showNotificationWithAction();
        }
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return false;

        String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void showNotificationWithAction() {
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, dialogIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("HelpMeChannel", "Help Requests", NotificationManager.IMPORTANCE_HIGH);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "HelpMeChannel")
                    .setContentTitle("Help Needed")
                    .setContentText("Tap to open the app for help.")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_sos_flash)
                    .setAutoCancel(true)
                    .build();
        }

        notificationManager.notify(1, notification); // Show the notification
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        return START_STICKY;
    }

    private Notification createNotification() {
        // Create a notification to keep the service in the foreground
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("SpeechRecognitionService", "Speech Recognition", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }


        return new Notification.Builder(this, "SpeechRecognitionService")
                .setContentTitle("Listening for Commands")
                .setContentText("The app is listening for voice commands.")
                .build();


    }

    private void openDialogActivity() {
        // Add the code to launch the dialog activity when "help me" is recognized.
        Intent dialogIntent = new Intent(this, DialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }
    private void handleEmergency(DataSnapshot sosSnapshot) {
        // Handle the emergency event (e.g., ring the phone)
        // You could start a Ringtone or send a notification
        playEmergencyRingtone();
    }

    private void playEmergencyRingtone() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.alert_aud);
        mediaPlayer.setLooping(false); // Loop the ringtone
        mediaPlayer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (sosReference != null && sosListener != null) {
            sosReference.removeEventListener(sosListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

