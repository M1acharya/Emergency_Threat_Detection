package com.example.tfai;

import java.util.ArrayList;

public class SOS {
    private String user_email;
    private ArrayList<String> contacts;
    private Object timestamp;

    public SOS() {
        // Default constructor required for calls to DataSnapshot.getValue(SOS.class)
    }

    public SOS(String user_email, Object timestamp) {
        this.user_email = user_email;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}

