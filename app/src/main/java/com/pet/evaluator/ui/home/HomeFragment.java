package com.pet.evaluator.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.pet.evaluator.R;
import com.pet.evaluator.SharedViewModel;

public class HomeFragment extends Fragment {
    
    private SharedViewModel sharedViewModel;
    private TextView resumeStatusTextView;
    private TextView videoStatusTextView;
    private TextView cgpaStatusTextView;
    private TextView evaluationStatusTextView;
    private Button startEvaluationButton;
    private CardView resumeCardView;
    private CardView videoCardView;
    private CardView resultsCardView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        resumeStatusTextView = view.findViewById(R.id.text_resume_status);
        videoStatusTextView = view.findViewById(R.id.text_video_status);
        cgpaStatusTextView = view.findViewById(R.id.text_cgpa_status);
        evaluationStatusTextView = view.findViewById(R.id.text_evaluation_status);
        startEvaluationButton = view.findViewById(R.id.button_start_evaluation);
        resumeCardView = view.findViewById(R.id.card_resume);
        videoCardView = view.findViewById(R.id.card_video);
        resultsCardView = view.findViewById(R.id.card_results);
        
        // Set click listeners for navigation
        resumeCardView.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.navigation_resume));
        
        videoCardView.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.navigation_video));
        
        resultsCardView.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.navigation_results));
        
        startEvaluationButton.setOnClickListener(v -> 
                Navigation.findNavController(view).navigate(R.id.navigation_resume));
        
        // Observe data changes
        observeViewModel();
    }
    
    private void observeViewModel() {
        // Resume status
        sharedViewModel.getIsResumeProcessed().observe(getViewLifecycleOwner(), isProcessed -> {
            if (Boolean.TRUE.equals(isProcessed)) {
                resumeStatusTextView.setText("✅ Resume analyzed");
                resumeStatusTextView.setTextColor(getResources().getColor(R.color.colorSuccess));
            } else {
                resumeStatusTextView.setText("⬜ Resume not analyzed");
                resumeStatusTextView.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            }
        });
        
        // CGPA status
        sharedViewModel.getCgpa().observe(getViewLifecycleOwner(), cgpa -> {
            if (cgpa != null) {
                cgpaStatusTextView.setText("✅ CGPA: " + cgpa);
                cgpaStatusTextView.setTextColor(getResources().getColor(R.color.colorSuccess));
            } else {
                cgpaStatusTextView.setText("⬜ CGPA not entered");
                cgpaStatusTextView.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            }
        });
        
        // Video status
        sharedViewModel.getIsVideoProcessed().observe(getViewLifecycleOwner(), isProcessed -> {
            if (Boolean.TRUE.equals(isProcessed)) {
                videoStatusTextView.setText("✅ Video analyzed");
                videoStatusTextView.setTextColor(getResources().getColor(R.color.colorSuccess));
            } else {
                videoStatusTextView.setText("⬜ Video not analyzed");
                videoStatusTextView.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            }
        });
        
        // Evaluation status
        sharedViewModel.getIsEvaluationComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (Boolean.TRUE.equals(isComplete)) {
                evaluationStatusTextView.setText("✅ Evaluation complete");
                evaluationStatusTextView.setTextColor(getResources().getColor(R.color.colorSuccess));
                startEvaluationButton.setText("View Results");
                startEvaluationButton.setOnClickListener(v -> 
                        Navigation.findNavController(requireView()).navigate(R.id.navigation_results));
            } else {
                evaluationStatusTextView.setText("⬜ Evaluation not performed");
                evaluationStatusTextView.setTextColor(getResources().getColor(R.color.colorSecondaryText));
            }
        });
    }
}
