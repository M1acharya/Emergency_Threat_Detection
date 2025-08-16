package com.example.tfai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    LinearLayout suggestionContainer;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.home_layout,container,false);
        // Get references to UI components
        Button emergencyButton = v.findViewById(R.id.btn_emergency);
        suggestionContainer = v.findViewById(R.id.suggestion_container);
        EditText customMessageInput = v.findViewById(R.id.et_custom_message);

        // Set click listener for the emergency button
        emergencyButton.setOnClickListener(view -> {
            // Show suggestion container when emergency button is clicked
            suggestionContainer.setVisibility(View.VISIBLE);
        });

        // Set click listeners for predefined emergency message buttons
        v.findViewById(R.id.btn_suggestion1).setOnClickListener(view -> sendEmergencyMessage("I need help, please respond urgently!"));
        v.findViewById(R.id.btn_suggestion2).setOnClickListener(view -> sendEmergencyMessage("I'm in danger, please send help!"));
        v.findViewById(R.id.btn_suggestion3).setOnClickListener(view -> sendEmergencyMessage("Please track my location for safety."));
        v.findViewById(R.id.btn_suggestion4).setOnClickListener(view -> sendEmergencyMessage("Alert! I need immediate assistance."));

        // Set click listener for the custom message send button
        v.findViewById(R.id.btn_send_custom_message).setOnClickListener(view -> {
            String customMessage = customMessageInput.getText().toString();
            if (!customMessage.isEmpty()) {
                sendEmergencyMessage(customMessage);
            } else {
                Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }

    private void sendEmergencyMessage(String message) {
        Toast.makeText(getContext(), "Emergency Message Sent: " + message, Toast.LENGTH_SHORT).show();
        openDialogActivity(message);
    }

    private void openDialogActivity(String message) {
        Intent dialogIntent = new Intent(requireContext(), DialogActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtra("emergency_message",message);
        suggestionContainer.setVisibility(View.GONE);
        startActivity(dialogIntent);
    }
}
