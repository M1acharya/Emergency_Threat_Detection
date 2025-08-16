package com.example.tfai;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;

public class RecorderService extends Service {
    private static final String CHANNEL_ID = "555";
    boolean recordAudio=true;

    private SpeechRecognizer speechRecognizer;
    DBHelper db;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        db=new DBHelper(getApplicationContext(),"ContactList");
        if (intent != null) {
            String action = intent.getAction();
            Log.d("SERVIECE",action+"");
            if(action!=null)
                switch (action) {
                    case "ACTION_START_FOREGROUND_SERVICE":
                        Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
                        //createNotification();
                        startListeningForCommands();
                        break;
                    case "TOGGLE_LIBRA":
//                        recordAudio=!recordAudio;
//                        if(recordAudio){
//                            Toast.makeText(getApplicationContext(), "Turning ON voice triggers..", Toast.LENGTH_LONG).show();
//                            recordAudio();
//                        }
//                        else{
//                            Toast.makeText(getApplicationContext(), "Turning OFF voice triggers..", Toast.LENGTH_LONG).show();
//
//                        }
                        break;
                    case "SOS_FLASH":
                        Toast.makeText(getApplicationContext(), "Initiating SOS..", Toast.LENGTH_LONG).show();
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        startSOSFlash();

                        break;

                    case "SMS_ALERT":
                        Toast.makeText(getApplicationContext(), "Sending SMS..", Toast.LENGTH_LONG).show();
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        sendAlertSMS();

                        break;

                    case "POLICE_ALERT":
                        Toast.makeText(getApplicationContext(), "Hang tight! We are calling the police...", Toast.LENGTH_LONG).show();
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        callPoliceNotif();

                        break;

                }

        }

        return super.onStartCommand(intent, flags, startId);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    void createNotification(){
        /*Intent sosFlash = new Intent(this, RecorderService.class);
        sosFlash.setAction("SOS_FLASH");
        sosFlash.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP ) ;

        PendingIntent pendingSOSIntent = PendingIntent.getService(this, 0, sosFlash, PendingIntent.FLAG_IMMUTABLE);

        Intent smsAlert = new Intent(this, RecorderService.class);
        smsAlert.setAction("SMS_ALERT");
        smsAlert.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP ) ;

        PendingIntent pendingSMSIntent = PendingIntent.getService(this, 0, smsAlert, PendingIntent.FLAG_IMMUTABLE);

        Intent policeAlert = new Intent(this, RecorderService.class);
        policeAlert.setAction("POLICE_ALERT");
        policeAlert.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP ) ;

        PendingIntent pendingPOLICEIntent = PendingIntent.getService(this, 0, policeAlert, PendingIntent.FLAG_IMMUTABLE);

        Intent toggleLibra = new Intent(this, RecorderService.class);
        toggleLibra.setAction("TOGGLE_LIBRA");
        toggleLibra.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP | Intent. FLAG_ACTIVITY_SINGLE_TOP ) ;

        PendingIntent pendingLibraIntent = PendingIntent.getService(this, 0, toggleLibra, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_notification_bar);
        remoteViews.setOnClickPendingIntent(R.id.toggle_libra,pendingLibraIntent);
        remoteViews.setOnClickPendingIntent(R.id.sos_flash,pendingSOSIntent);
        remoteViews.setOnClickPendingIntent(R.id.sms_alert,pendingSMSIntent);
        remoteViews.setOnClickPendingIntent(R.id.police_alert,pendingPOLICEIntent);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText("Test")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setContent(remoteViews)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        startForeground(1, notification);*/
        recordAudio();

    }
    AudioRecord microphone;
    short[] buffer = new short[22050];
    public void recordAudio(){
        float chunkDuration= (float) 0.5;
        int sampleRate=44100;
        int chunkSamples= (int) (chunkDuration*sampleRate);
        int feedSamples=sampleRate*10;
        int minBufferSize = chunkSamples;

        microphone = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,  AudioFormat.ENCODING_PCM_16BIT, chunkSamples*2);
        microphone.startRecording();
        Log.d("Mic", microphone.getBufferSizeInFrames()+"");

        if(!Python.isStarted())
            Python.start(new AndroidPlatform(this));
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
//                    if(recordAudio==false){
//                        Toast.makeText(RecorderService.this, "Turning off voice triggers", Toast.LENGTH_SHORT).show();
//                        break;
//                    }

                //                        Thread.sleep(5);
                    int readSize = microphone.read(buffer, 0, buffer.length);
                        Python py=Python.getInstance();
                        PyObject pyobj=py.getModule("test");
                        PyObject oj=pyobj.callAttr("processData",buffer);

                        if(oj!=null)
                            if(oj.toInt() == 1){
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText(RecorderService.this, "Initializing safety measures", Toast.LENGTH_LONG).show();

                                        Intent dialogIntent = new Intent(getApplicationContext(), DialogActivity.class);
                                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(dialogIntent);



                                    }
                                });
                            }

                }
            }
        }).start();

//        microphone.stop();



    }
    void callPoliceNotif(){
        Uri number = Uri.parse("tel:1091");
        Intent callIntent = new Intent(Intent.ACTION_CALL, number);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(callIntent);
    }

    void startSOSFlash(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
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
                    try {
                        Thread.sleep(blinkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        },2000);

    }
    void sendAlertSMS(){
        String helpMessage="Hey,I am not feeling safe here. Do you mind calling me? Please respond ASAP.";
        ArrayList<String> contacts=db.getAllContacts();
        SmsManager smsManager = SmsManager.getDefault();
        for(String cont:contacts){
            try {
                smsManager.sendTextMessage(cont, null, helpMessage, null, null);
            } catch (Exception ex) {
                Toast.makeText(this, "Error sending message to"+cont, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void startListeningForCommands() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d("sosApp","text from voce"+matches);
                if (matches != null && matches.contains("help me")) {
                    openDialogActivity();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }

            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "No match found";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "Recognizer is busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "No speech input";
                        break;
                    default:
                        message = "Unknown error";
                        break;
                }
                Log.e("SpeechRecognizer", "Error: " + message);
            }


            // Implement other methods as needed...
        });

        speechRecognizer.startListening(intent);
    }

    private void openDialogActivity() {
        Intent dialogIntent = new Intent(getApplicationContext(), DialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    // Don't forget to stop listening when you're done
    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
    }



}
