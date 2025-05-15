package com.pet.evaluator.ui.results;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pet.evaluator.EvaluationResult;
import com.pet.evaluator.R;
import com.pet.evaluator.SharedViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultsFragment extends Fragment {

    private ResultsViewModel resultsViewModel;
    private SharedViewModel sharedViewModel;

    private TextView overallScoreTextView;
    private TextView resumeScoreTextView;
    private TextView academicScoreTextView;
    private TextView fluencyScoreTextView;
    private TextView vocabularyScoreTextView;
    private TextView detailedAnalysisTextView;
    private ProgressBar resumeProgressBar;
    private ProgressBar academicProgressBar;
    private ProgressBar fluencyProgressBar;
    private ProgressBar vocabularyProgressBar;

    private Button saveReportButton;
    private Button shareReportButton;
    private Button newEvaluationButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        // Get ViewModels
        resultsViewModel = new ViewModelProvider(this).get(ResultsViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        return inflater.inflate(R.layout.fragment_results, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        overallScoreTextView = view.findViewById(R.id.text_overall_score);
        resumeScoreTextView = view.findViewById(R.id.text_resume_score);
        academicScoreTextView = view.findViewById(R.id.text_academic_score);
        fluencyScoreTextView = view.findViewById(R.id.text_fluency_score);
        vocabularyScoreTextView = view.findViewById(R.id.text_vocabulary_score);
        detailedAnalysisTextView = view.findViewById(R.id.text_detailed_analysis);

        resumeProgressBar = view.findViewById(R.id.progress_resume_score);
        academicProgressBar = view.findViewById(R.id.progress_academic_score);
        fluencyProgressBar = view.findViewById(R.id.progress_fluency_score);
        vocabularyProgressBar = view.findViewById(R.id.progress_vocabulary_score);
        saveReportButton = view.findViewById(R.id.button_save_report);
        shareReportButton = view.findViewById(R.id.button_share_report);
        newEvaluationButton = view.findViewById(R.id.button_new_evaluation);
        // Set click listeners
        saveReportButton.setOnClickListener(v -> saveReport());
        shareReportButton.setOnClickListener(v -> shareReport());
        newEvaluationButton.setOnClickListener(v -> startNewEvaluation());

        // Observe the SharedViewModel for evaluation results
        observeSharedViewModel();
    }

    private void observeSharedViewModel() {
        // Observe evaluation state
        sharedViewModel.getIsEvaluationComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (Boolean.TRUE.equals(isComplete)) {
                // If evaluation is complete, display the results
                displayResults();
            }
        });

        // Observe evaluation results
        sharedViewModel.getEvaluationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                displayResultFromEvaluationResult(result);
            } else {
                // If there's no result yet but we're on this screen, use mock data
                displayMockResults();
            }
        });

        // Observe processing state
        sharedViewModel.getIsProcessing().observe(getViewLifecycleOwner(), isProcessing -> {
            if (Boolean.TRUE.equals(isProcessing)) {
                // Show loading UI
                showLoadingState();
            } else {
                // Show content UI
                showContentState();
            }
        });
        // Observe any error messages
        sharedViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            // Use null and empty string safety check
            if (error != null && !error.trim().isEmpty()) {
                Context context = getContext();
                if (context != null) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayResultFromEvaluationResult(EvaluationResult result) { // Extract scores from the evaluation
                                                                              // result with null safety
        float resumeScore = result.getScore() / 100f; // Convert to 0-1 scale
        float cgpa = sharedViewModel.getCgpa().getValue() != null ? sharedViewModel.getCgpa().getValue() / 10f : 0.85f; // Convert
                                                                                                                        // CGPA
                                                                                                                        // to
                                                                                                                        // 0-1
                                                                                                                        // scale
        // Use safe accessors that don't return null
        float fluencyScore = result.getFluencySafe().getOverallScore() / 100f;
        float vocabularyScore = result.getVocabularySafe().getOverallScore() / 100f;

        // Calculate overall score
        float overallScore = result.getOverallScore() / 100f;
        if (overallScore <= 0) {
            // If not provided, calculate it
            overallScore = resultsViewModel.calculateOverallScore(resumeScore, cgpa, fluencyScore, vocabularyScore);
        }

        // Update the UI
        updateScoreUI(overallScore, resumeScore, cgpa, fluencyScore, vocabularyScore);
        // Display detailed analysis with null safety
        String detailedAnalysis = result.getRecommendationSafe();
        if (detailedAnalysis.isEmpty()) {
            detailedAnalysis = resultsViewModel.generateDetailedAnalysis(resumeScore, cgpa, fluencyScore,
                    vocabularyScore);
        }
        detailedAnalysisTextView.setText(detailedAnalysis);
    }

    private void displayMockResults() {
        // Mock data for demonstration
        float resumeScore = 0.8f; // 80%
        float cgpa = sharedViewModel.getCgpa().getValue() != null ? sharedViewModel.getCgpa().getValue() / 10f : 0.85f; // Convert
                                                                                                                        // CGPA
                                                                                                                        // to
                                                                                                                        // a
                                                                                                                        // percentage
                                                                                                                        // (85%)
        float fluencyScore = 0.75f; // 75%
        float vocabularyScore = 0.85f; // 85%

        // Calculate overall score (weighted average)
        float overallScore = resultsViewModel.calculateOverallScore(resumeScore, cgpa, fluencyScore, vocabularyScore);

        // Update the UI
        updateScoreUI(overallScore, resumeScore, cgpa, fluencyScore, vocabularyScore);

        // Generate and display detailed analysis
        String detailedAnalysis = resultsViewModel.generateDetailedAnalysis(resumeScore, cgpa, fluencyScore,
                vocabularyScore);
        detailedAnalysisTextView.setText(detailedAnalysis);
    }

    private void displayResults() {
        EvaluationResult result = sharedViewModel.getEvaluationResult().getValue();
        if (result != null) {
            displayResultFromEvaluationResult(result);
        } else {
            displayMockResults();
        }
    }

    private void updateScoreUI(float overallScore, float resumeScore, float cgpa, float fluencyScore,
            float vocabularyScore) {
        // Convert scores to percentages for display
        int overallPercent = Math.round(overallScore * 100);
        int resumePercent = Math.round(resumeScore * 100);
        int academicPercent = Math.round(cgpa * 100);
        int fluencyPercent = Math.round(fluencyScore * 100);
        int vocabularyPercent = Math.round(vocabularyScore * 100);

        // Update text views
        overallScoreTextView.setText(overallPercent + "%");
        resumeScoreTextView.setText(resumePercent + "%");
        academicScoreTextView.setText(academicPercent + "%");
        fluencyScoreTextView.setText(fluencyPercent + "%");
        vocabularyScoreTextView.setText(vocabularyPercent + "%");

        // Update progress bars
        resumeProgressBar.setProgress(resumePercent);
        academicProgressBar.setProgress(academicPercent);
        fluencyProgressBar.setProgress(fluencyPercent);
        vocabularyProgressBar.setProgress(vocabularyPercent);
    }

    private void saveReport() {
        try {
            File reportFile = generateReportFile();
            Toast.makeText(getContext(), "Report saved: " + reportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving report", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport() {
        try {
            File reportFile = generateReportFile();

            Uri reportUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.pet.evaluator.fileprovider",
                    reportFile);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, reportUri);
            shareIntent.setType("text/plain");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Report"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error sharing report", Toast.LENGTH_SHORT).show();
        }
    }

    private File generateReportFile() throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());

        String fileName = "PET_Report_" + timestamp + ".txt";
        File reportFile = new File(requireContext().getExternalFilesDir(null), fileName);

        // Get transcription data if available
        String transcription = "";
        EvaluationResult result = sharedViewModel.getEvaluationResult().getValue();
        if (result != null && result.getTranscription() != null) {
            transcription = "\nTRANSCRIPTION:\n" + result.getTranscription() + "\n\n";
        }

        // Get resume filename if available
        String resumeFilename = "";
        Uri resumeUri = sharedViewModel.getResumeUri().getValue();
        if (resumeUri != null) {
            resumeFilename = "\nResume File: " + resumeUri.getLastPathSegment() + "\n";
        }

        // Build the report content
        String reportContent = "Performance Evaluation Report\n" +
                "==========================\n\n" +
                "Generated: " + new Date().toString() + resumeFilename + "\n\n" +
                "SCORES:\n" +
                "Overall: " + overallScoreTextView.getText() + "\n" +
                "Resume: " + resumeScoreTextView.getText() + "\n" +
                "Academic: " + academicScoreTextView.getText() + "\n" +
                "English Fluency: " + fluencyScoreTextView.getText() + "\n" +
                "Vocabulary: " + vocabularyScoreTextView.getText() + "\n\n" +
                "DETAILED ANALYSIS:\n" +
                detailedAnalysisTextView.getText() +
                transcription;

        try (FileOutputStream fos = new FileOutputStream(reportFile)) {
            fos.write(reportContent.getBytes());
        }
        return reportFile;
    }

    /**
     * Resets the evaluation process and navigates back to Home screen
     */
    private void startNewEvaluation() {
        // Reset evaluation data in SharedViewModel
        sharedViewModel.resetEvaluationData();

        // Navigate back to home screen
        androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.navigation_home);

        Toast.makeText(getContext(), "Ready for new evaluation", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show loading state in the UI
     */
    private void showLoadingState() {
        // Disable buttons during processing
        saveReportButton.setEnabled(false);
        shareReportButton.setEnabled(false);
        newEvaluationButton.setEnabled(false);

        // Show indeterminate progress in progress bars
        resumeProgressBar.setIndeterminate(true);
        academicProgressBar.setIndeterminate(true);
        fluencyProgressBar.setIndeterminate(true);
        vocabularyProgressBar.setIndeterminate(true);

        // Set placeholder text
        detailedAnalysisTextView.setText("Processing evaluation data...");
    }

    /**
     * Show content state in the UI
     */
    private void showContentState() {
        // Enable buttons when processing is complete
        saveReportButton.setEnabled(true);
        shareReportButton.setEnabled(true);
        newEvaluationButton.setEnabled(true);

        // Set progress bars to determinate mode
        resumeProgressBar.setIndeterminate(false);
        academicProgressBar.setIndeterminate(false);
        fluencyProgressBar.setIndeterminate(false);
        vocabularyProgressBar.setIndeterminate(false);

        // Display results (already handled by observeSharedViewModel)
    }
}
