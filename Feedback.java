package com.example.tfai;

// Feedback.java
public class Feedback {
    private String userId;
    private float rating;
    private String comment;

    public Feedback() {
        // Default constructor required for calls to DataSnapshot.getValue(Feedback.class)
    }

    public Feedback(String userId, float rating, String comment) {
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getUserId() {
        return userId;
    }

    public float getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}

