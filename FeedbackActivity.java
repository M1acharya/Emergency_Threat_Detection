package com.example.tfai;

// FeedbackActivity.java
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText feedbackComment;
    private Button submitFeedbackButton;
    private DatabaseReference feedbackReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ratingBar = findViewById(R.id.ratingBar);
        feedbackComment = findViewById(R.id.feedbackComment);
        submitFeedbackButton = findViewById(R.id.submitFeedbackButton);

        feedbackReference = FirebaseDatabase.getInstance().getReference("user_feedback");

        submitFeedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitFeedback();
            }
        });
    }

    private void submitFeedback() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        float rating = ratingBar.getRating();
        String comment = feedbackComment.getText().toString().trim();

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please enter your comments.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Feedback object
        Feedback feedback = new Feedback(userId, rating, comment);

        // Store feedback in Firebase
        feedbackReference.push().setValue(feedback)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(FeedbackActivity.this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    } else {
                        Toast.makeText(FeedbackActivity.this, "Failed to submit feedback: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
