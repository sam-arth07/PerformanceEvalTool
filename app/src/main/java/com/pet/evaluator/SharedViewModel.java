package com.pet.evaluator;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Shared ViewModel to manage the evaluation process across fragments
 */
public class SharedViewModel extends AndroidViewModel {

    // Evaluation Service for performing ML model evaluations
    private final EvaluationService evaluationService;

    // Resume data
    private final MutableLiveData<Uri> resumeUri = new MutableLiveData<>();
    private final MutableLiveData<Float> cgpa = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isResumeProcessed = new MutableLiveData<>(false);

    // Video data
    private final MutableLiveData<Uri> videoUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isVideoProcessed = new MutableLiveData<>(false); // Evaluation state and
                                                                                            // results
    private final MutableLiveData<Boolean> isEvaluationComplete = new MutableLiveData<>(false);
    private final MutableLiveData<EvaluationResult> evaluationResult = new MutableLiveData<>();

    // Offline mode status
    private final MutableLiveData<Boolean> isOfflineMode = new MutableLiveData<>(false);

    public SharedViewModel(@NonNull Application application) {
        super(application);
        evaluationService = new EvaluationService(application);

        // Observe the evaluation service results
        observeEvaluationService();
    }

    private void observeEvaluationService() {
        // Resume data observations
        evaluationService.getResumeData().observeForever(resumeData -> {
            if (resumeData != null && resumeData.isProcessed) {
                isResumeProcessed.setValue(true);
                checkCompletionState();
            }
        });

        // Video data observations
        evaluationService.getVideoData().observeForever(videoData -> {
            if (videoData != null && videoData.isProcessed) {
                isVideoProcessed.setValue(true);
                checkCompletionState();
            }
        });

        // Result observation
        evaluationService.getEvaluationResult().observeForever(result -> {
            if (result != null) {
                evaluationResult.setValue(result);
                isEvaluationComplete.setValue(true);
            }
        });
    }

    /**
     * Check if all components are processed and ready for final evaluation
     */
    private void checkCompletionState() {
        Boolean resumeReady = isResumeProcessed.getValue();
        Boolean videoReady = isVideoProcessed.getValue();

        if (Boolean.TRUE.equals(resumeReady) && Boolean.TRUE.equals(videoReady)) {
            // If both resume and video are processed, we can evaluate
            Uri resume = resumeUri.getValue();
            Uri video = videoUri.getValue();
            Float cgpaValue = cgpa.getValue();

            if (resume != null && video != null && cgpaValue != null) {
                evaluationService.evaluateCandidate(resume, video, cgpaValue);
            }
        }
    }

    /**
     * Process a resume for evaluation
     *
     * @param uri       Resume file URI
     * @param cgpaValue Candidate's CGPA
     */
    public void processResume(Uri uri, float cgpaValue) {
        resumeUri.setValue(uri);
        cgpa.setValue(cgpaValue);
        evaluationService.processResume(uri);
    }

    /**
     * Process a video for evaluation
     *
     * @param uri Video file URI
     */
    public void processVideo(Uri uri) {
        videoUri.setValue(uri);
        evaluationService.processVideo(uri);
    }

    /**
     * Force evaluation with current data
     */
    public void evaluateWithCurrentData() {
        Uri resume = resumeUri.getValue();
        Uri video = videoUri.getValue();
        Float cgpaValue = cgpa.getValue();

        if (resume != null && video != null && cgpaValue != null) {
            evaluationService.evaluateCandidate(resume, video, cgpaValue);
        }
    }

    // Getters for LiveData objects

    public LiveData<Uri> getResumeUri() {
        return resumeUri;
    }

    public LiveData<Float> getCgpa() {
        return cgpa;
    }

    public LiveData<Boolean> getIsResumeProcessed() {
        return isResumeProcessed;
    }

    public LiveData<Uri> getVideoUri() {
        return videoUri;
    }

    public LiveData<Boolean> getIsVideoProcessed() {
        return isVideoProcessed;
    }

    public LiveData<Boolean> getIsEvaluationComplete() {
        return isEvaluationComplete;
    }

    public LiveData<EvaluationResult> getEvaluationResult() {
        return evaluationResult;
    }

    public LiveData<Boolean> getIsProcessing() {
        return evaluationService.getIsProcessing();
    }

    public LiveData<String> getErrorMessage() {
        return evaluationService.getErrorMessage();
    }

    /**
     * Reset all evaluation data to start a new evaluation
     */
    public void resetEvaluationData() {
        // Clear resume data
        resumeUri.setValue(null);
        cgpa.setValue(null);
        isResumeProcessed.setValue(false);

        // Clear video data
        videoUri.setValue(null);
        isVideoProcessed.setValue(false);

        // Clear evaluation state
        isEvaluationComplete.setValue(false);
        evaluationResult.setValue(null);

        // Reset the evaluation service
        evaluationService.reset();
    }

    /**
     * Get the offline mode status
     * 
     * @return LiveData for offline mode status
     */
    public LiveData<Boolean> getIsOfflineMode() {
        return isOfflineMode;
    }

    /**
     * Set the offline mode status
     * 
     * @param value true if offline mode is enabled, false otherwise
     */
    public void setIsOfflineMode(boolean value) {
        isOfflineMode.setValue(value);

        // Update EvaluationService with this setting
        if (evaluationService != null) {
            evaluationService.setUseOfflineMode(value);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        evaluationService.cleanup();
    }
}
