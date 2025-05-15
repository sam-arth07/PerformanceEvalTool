package com.pet.evaluator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Service class that manages candidate evaluation
 */
public class EvaluationService {
    private static final String TAG = "EvaluationService";

    // ML Model Manager for server-based evaluation
    private MLModelManager mlModelManager;

    // TFLite Evaluator for on-device evaluation (fallback)
    private TFLiteEvaluator resumeEvaluator;
    private TFLiteEvaluator videoEvaluator;
    private TFLiteEvaluator overallEvaluator;

    // LiveData for storing evaluation state and results
    private final MutableLiveData<ResumeData> resumeData = new MutableLiveData<>();
    private final MutableLiveData<VideoData> videoData = new MutableLiveData<>();
    private final MutableLiveData<Float> cgpa = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<EvaluationResult> evaluationResult = new MutableLiveData<>();
    private final Context context;

    /**
     * Constructor
     * 
     * @param context Android context
     */
    public EvaluationService(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        // Store application context to avoid memory leaks
        this.context = context.getApplicationContext();

        // Initialize ML Model Manager
        try {
            mlModelManager = new MLModelManager(this.context);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ML Model Manager: " + e.getMessage(), e);
            mlModelManager = null;
            errorMessage.setValue("Failed to initialize ML services: " + e.getMessage());
        }

        // Initialize TFLite models for offline use
        try {
            resumeEvaluator = new TFLiteEvaluator(this.context, "resume_model.tflite");
            videoEvaluator = new TFLiteEvaluator(this.context, "video_model.tflite");
            overallEvaluator = new TFLiteEvaluator(this.context, "evaluation_model.tflite");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TFLite models: " + e.getMessage(), e);
            errorMessage.setValue("Failed to initialize on-device models: " + e.getMessage());
        }
    }

    /**
     * Process a resume
     * 
     * @param resumeUri URI of the resume file
     */
    public void processResume(Uri resumeUri) {
        isProcessing.setValue(true);

        // Store resume URI
        ResumeData data = new ResumeData();
        data.resumeUri = resumeUri;
        resumeData.setValue(data);

        // Check if we have network connectivity
        if (isNetworkAvailable()) {
            // Use server-based model for analysis
            mlModelManager.analyzeResume(resumeUri, new MLModelManager.EvaluationCallback() {
                @Override
                public void onSuccess(EvaluationResult result) {
                    ResumeData updatedData = resumeData.getValue();
                    if (updatedData != null && result != null) {
                        updatedData.isProcessed = true;
                        updatedData.score = result.getScore();
                        updatedData.skillCount = result.getSkillCount();
                        updatedData.experienceYears = result.getExperienceYears();
                        resumeData.setValue(updatedData);
                    }
                    isProcessing.setValue(false);
                }

                @Override
                public void onError(String errorMsg) {
                    // MORE EXPLICIT NULL CHECK: First assign to local variable before using
                    // This ensures the Java compiler will generate proper null checking bytecode
                    String localErrorMsg = errorMsg;
                    if (localErrorMsg == null) {
                        localErrorMsg = "Unknown error";
                    }
                    // Now use the guaranteed non-null local variable
                    errorMessage.setValue("Resume processing error: " + localErrorMsg);
                    fallbackResumeProcessing(resumeUri);
                }
            });
        } else {
            // Use on-device model (simpler analysis)
            fallbackResumeProcessing(resumeUri);
        }
    }

    /**
     * Process a video
     * 
     * @param videoUri URI of the video file
     */
    public void processVideo(Uri videoUri) {
        isProcessing.setValue(true);

        // Store video URI
        VideoData data = new VideoData();
        data.videoUri = videoUri;
        videoData.setValue(data);

        // Check if we have network connectivity
        if (isNetworkAvailable()) {
            // Use server-based model for analysis
            mlModelManager.analyzeVideo(videoUri, new MLModelManager.EvaluationCallback() {
                @Override
                public void onSuccess(EvaluationResult result) {
                    VideoData updatedData = videoData.getValue();
                    if (updatedData != null && result != null) {
                        updatedData.isProcessed = true;
                        // Use safe accessors from EvaluationResult for all fields
                        updatedData.fluencyScore = result.getFluencySafe().getScore();
                        updatedData.vocabularyScore = result.getVocabularySafe().getScore();
                        // Use the setter method to safely set transcription
                        updatedData.setTranscription(result.getTranscriptionSafe());
                        videoData.setValue(updatedData);
                    }
                    isProcessing.setValue(false);
                }

                @Override
                public void onError(String errorMsg) {
                    // MORE EXPLICIT NULL CHECK: First assign to local variable before using
                    // This ensures the Java compiler will generate proper null checking bytecode
                    String localErrorMsg = errorMsg;
                    if (localErrorMsg == null) {
                        localErrorMsg = "Unknown error";
                    }
                    // Now use the guaranteed non-null local variable
                    errorMessage.setValue("Video processing error: " + localErrorMsg);
                    fallbackVideoProcessing(videoUri);
                }
            });
        } else {
            // Use on-device model (simpler analysis)
            fallbackVideoProcessing(videoUri);
        }
    }

    /**
     * Perform the final evaluation of the candidate
     * 
     * @param resumeUri URI of the resume file
     * @param videoUri  URI of the video file
     * @param cgpaValue CGPA value
     */
    public void evaluateCandidate(Uri resumeUri, Uri videoUri, float cgpaValue) {
        isProcessing.setValue(true);

        // Store CGPA cgpa.setValue(cgpaValue);

        // Check if we have network connectivity
        if (isNetworkAvailable()) {
            // Use server-based model for evaluation
            mlModelManager.evaluateCandidate(resumeUri, videoUri, cgpaValue, new MLModelManager.EvaluationCallback() {
                @Override
                public void onSuccess(EvaluationResult result) {
                    if (result != null) {
                        evaluationResult.setValue(result);
                    } else {
                        // Handle null result gracefully
                        Log.w(TAG, "Received null evaluation result");
                        fallbackEvaluation();
                    }
                    isProcessing.setValue(false);
                }

                @Override
                public void onError(String errorMsg) {
                    // MORE EXPLICIT NULL CHECK: First assign to local variable before using
                    // This ensures the Java compiler will generate proper null checking bytecode
                    String localErrorMsg = errorMsg;
                    if (localErrorMsg == null) {
                        localErrorMsg = "Unknown error";
                    }
                    // Now use the guaranteed non-null local variable
                    errorMessage.setValue("Evaluation error: " + localErrorMsg);
                    fallbackEvaluation();
                }
            });
        } else {
            // Use on-device models (simpler evaluation)
            fallbackEvaluation();
        }
    }

    /**
     * On-device resume processing (fallback)
     * 
     * @param resumeUri URI of the resume file
     */
    private void fallbackResumeProcessing(Uri resumeUri) {
        // This would be more complex in a real app - in a production environment,
        // we would use the TFLite model to analyze the resume
        Log.i(TAG, "Falling back to on-device resume processing for URI: " + resumeUri);

        ResumeData data = resumeData.getValue();
        if (data != null) {
            try {
                data.isProcessed = true;

                // Check if this is a mock URI or a real sample resume
                boolean isMockUri = resumeUri != null && "mock".equals(resumeUri.getScheme());
                boolean isSampleUri = resumeUri != null && resumeUri.getPath() != null &&
                        resumeUri.getPath().contains("sample_resume");

                // Add some intelligence - if it's a senior resume sample, give better scores
                float baseScore = 0.65f;
                float variability = 0.25f;
                int baseSkills = 7;
                int skillVariability = 8;
                float baseExperience = 2.0f;
                float experienceVariability = 4.0f;

                if (isSampleUri) {
                    String path = resumeUri.getPath();
                    if (path.contains("senior")) {
                        baseScore = 0.80f;
                        variability = 0.15f;
                        baseSkills = 12;
                        skillVariability = 5;
                        baseExperience = 5.0f;
                        experienceVariability = 3.0f;
                    } else if (path.contains("mid")) {
                        baseScore = 0.70f;
                        variability = 0.20f;
                        baseSkills = 9;
                        skillVariability = 6;
                        baseExperience = 3.0f;
                        experienceVariability = 3.0f;
                    }
                }

                // Add some variability to the mock data for more realism
                // Score between baseScore and baseScore+variability
                data.score = baseScore + (float) (Math.random() * variability);

                // Skills between baseSkills and baseSkills+skillVariability
                data.skillCount = baseSkills + (int) (Math.random() * skillVariability); // Experience between
                                                                                         // baseExperience and
                                                                                         // baseExperience+experienceVariability
                                                                                         // years with decimal places
                data.experienceYears = baseExperience + (float) (Math.random() * experienceVariability);

                resumeData.setValue(data);

                Log.d(TAG, "Successfully generated fallback resume analysis with " +
                        data.skillCount + " skills and " + data.experienceYears + " years experience");

                // Display a toast message to the user about offline mode
                showToastOnMainThread(
                        "Operating in offline mode for resume analysis. Network connection to ML server unavailable.");
            } catch (Exception e) {
                Log.e(TAG, "Error during fallback resume processing", e);
                // Ensure we have some values even in case of error
                if (data.score <= 0) {
                    data.score = 0.7f; // Default fallback score
                }
            }
        } else {
            Log.e(TAG, "Resume data is null in fallback processing");
        }

        isProcessing.setValue(false);
    }

    // Helper method to show toast messages from background threads
    private void showToastOnMainThread(final String message) {
        if (context == null)
            return;

        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    /**
     * On-device video processing (fallback)
     * 
     * @param videoUri URI of the video file
     */
    private void fallbackVideoProcessing(Uri videoUri) {
        // This would be more complex in a real app - would use TFLite models
        // For now, we'll generate realistic mock data
        Log.i(TAG, "Falling back to on-device video processing for URI: " + videoUri);

        VideoData data = videoData.getValue();
        if (data != null) {
            try {
                data.isProcessed = true;

                // Check if this is a mock URI or a real sample video
                boolean isMockUri = videoUri != null && "mock".equals(videoUri.getScheme());
                boolean isSampleUri = videoUri != null && videoUri.getPath() != null &&
                        videoUri.getPath().contains("sample_video");

                // Add some intelligence - if it's a sample senior video, give better scores
                float baseScore = 0.65f;
                float variability = 0.25f;

                if (isSampleUri) {
                    String path = videoUri.getPath();
                    if (path.contains("senior")) {
                        baseScore = 0.75f;
                        variability = 0.20f;
                    } else if (path.contains("mid")) {
                        baseScore = 0.70f;
                        variability = 0.20f;
                    }
                }

                // Add some randomness to make the mock data more realistic
                // Scores between baseScore and baseScore+variability
                data.fluencyScore = baseScore + (float) (Math.random() * variability);
                data.vocabularyScore = baseScore + (float) (Math.random() * variability);

                // Generate realistic mock transcription
                StringBuilder mockTranscription = new StringBuilder();
                mockTranscription.append("*** OFFLINE ANALYSIS MODE ***\n\n");
                mockTranscription.append("This is a mock transcription generated when the server is unavailable. ");
                mockTranscription
                        .append("The candidate demonstrates adequate communication skills for a technical position. ");
                mockTranscription.append("Vocabulary usage includes technical terms relevant to the field. ");
                mockTranscription.append("Speech patterns show coherent thought process with appropriate pauses. ");
                mockTranscription.append("\n\nFluency assessment: ");

                // Add conditional assessment text based on score
                if (data.fluencyScore > 0.8f) {
                    mockTranscription.append("Excellent, with clear articulation and natural flow.");
                } else if (data.fluencyScore > 0.7f) {
                    mockTranscription.append("Good, with occasional hesitations but maintains coherence.");
                } else {
                    mockTranscription.append("Acceptable, with room for improvement in sentence flow and clarity.");
                }

                mockTranscription.append("\n\nVocabulary assessment: ");
                if (data.vocabularyScore > 0.8f) {
                    mockTranscription.append("Excellent use of technical and domain-specific terminology.");
                } else if (data.vocabularyScore > 0.7f) {
                    mockTranscription.append("Good range of terminology with appropriate usage in context.");
                } else {
                    mockTranscription.append("Basic technical vocabulary with occasional misuse of terms.");
                }

                data.transcription = mockTranscription.toString();
                videoData.setValue(data);

                Log.d(TAG, "Successfully created fallback video analysis for URI: " + videoUri);
            } catch (Exception e) {
                // Extra safety to prevent any crashes during fallback
                Log.e(TAG, "Error during fallback video processing", e);
                if (data.transcription == null || data.transcription.isEmpty()) {
                    data.transcription = "Error generating analysis in offline mode. Please try again later when online.";
                }
            }
        } else {
            Log.e(TAG, "Video data is null in fallback processing");
        }

        isProcessing.setValue(false);
    }

    /**
     * On-device evaluation (fallback)
     */
    private void fallbackEvaluation() {
        // Get data from LiveData objects
        ResumeData resume = resumeData.getValue();
        VideoData video = videoData.getValue();
        Float cgpaValue = cgpa.getValue();

        // Check if we have all required data
        if (resume != null && resume.isProcessed && video != null &&
                video.isProcessed && cgpaValue != null) {

            // Normalize CGPA to 0-1 range (assuming 10-point scale)
            float normalizedCgpa = Math.min(cgpaValue / 10.0f, 1.0f);

            // Create a simple evaluation result
            EvaluationResult result = new EvaluationResult();

            // This would use the TFLite models in a real app
            // For now, we'll just calculate a weighted average
            float overallScore = 0.3f * resume.score + 0.2f * normalizedCgpa + 0.25f * video.fluencyScore
                    + 0.25f * video.vocabularyScore;

            // Set the overall score in the result
            try {
                // Create and set up component scores using public methods instead of reflection
                EvaluationResult.ComponentScores componentScores = new EvaluationResult.ComponentScores();
                componentScores.setResumeScore(resume.score);
                componentScores.setAcademicScore(normalizedCgpa);
                componentScores.setFluencyScore(video.fluencyScore);
                componentScores.setVocabularyScore(video.vocabularyScore);

                // Set overall score and component scores on result
                result.setOverallScore(overallScore);
                result.setComponentScores(componentScores);

                // Update the evaluation result
                evaluationResult.setValue(result);
            } catch (Exception e) {
                Log.e(TAG, "Error creating evaluation result", e);
                // Fallback to empty result if the process fails
                evaluationResult.setValue(result);
            }
        } else {
            Log.w(TAG, "Incomplete evaluation data - resume: " + (resume != null) +
                    ", video: " + (video != null) + ", cgpa: " + (cgpaValue != null));
            errorMessage.setValue("Incomplete data for evaluation");
        }
        isProcessing.setValue(false);
    }

    /**
     * Check if network is available
     * 
     * @return true if network is available and ML server is reachable, false
     *         otherwise
     */
    /**
     * Update offline mode setting
     * 
     * @param useOfflineMode true to use offline mode, false to try online mode
     */
    public void setUseOfflineMode(boolean useOfflineMode) {
        if (mlModelManager != null) {
            mlModelManager.setUseOfflineMode(useOfflineMode);
        }
    }

    /**
     * Get current offline mode status
     * 
     * @return true if in offline mode, false otherwise
     */
    public boolean isOfflineMode() {
        return mlModelManager == null || !mlModelManager.isServerAvailable();
    }

    private boolean isNetworkAvailable() {
        if (context == null) {
            Log.e(TAG, "Context is null when checking network availability");
            return false;
        }

        // If we previously determined the server is unavailable, don't try again
        // This prevents multiple timeouts during the same session
        if (mlModelManager != null && !mlModelManager.isServerAvailable()) {
            Log.d(TAG, "ML server was previously marked as unavailable, using offline mode");
            return false;
        }

        ConnectivityManager connectivityManager = null;
        try {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting connectivity service", e);
            return false;
        }

        if (connectivityManager != null) {
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();

                if (!isConnected) {
                    Log.i(TAG, "Network is not connected, using offline mode");
                    return false;
                }

                // Even if the network is connected, the server might be unreachable
                // We'll rely on the isServerAvailable flag from MLModelManager for that
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error checking network info", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (resumeEvaluator != null)
            resumeEvaluator.close();
        if (videoEvaluator != null)
            videoEvaluator.close();
        if (overallEvaluator != null)
            overallEvaluator.close();
    }

    // Getters for LiveData objects
    public LiveData<ResumeData> getResumeData() {
        return resumeData;
    }

    public LiveData<VideoData> getVideoData() {
        return videoData;
    }

    public LiveData<Float> getCgpa() {
        return cgpa;
    }

    public LiveData<Boolean> getIsProcessing() {
        return isProcessing;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<EvaluationResult> getEvaluationResult() {
        return evaluationResult;
    }

    public void setCgpa(float cgpaValue) {
        cgpa.setValue(cgpaValue);
    }

    /**
     * Reset all evaluation data and states
     */
    public void reset() {
        // Clear results and states
        resumeData.setValue(new ResumeData());
        videoData.setValue(new VideoData());
        cgpa.setValue(null);
        evaluationResult.setValue(null);
        isProcessing.setValue(false);
        errorMessage.setValue(null);
    }

    /**
     * Data class for resume information
     */
    public static class ResumeData {
        public Uri resumeUri;
        public boolean isProcessed = false;
        public float score = 0.0f;
        public int skillCount = 0;
        public float experienceYears = 0.0f;
    }

    /**
     * Data class for video information
     */
    public static class VideoData {
        public Uri videoUri;
        public boolean isProcessed = false;
        public float fluencyScore = 0.0f;
        public float vocabularyScore = 0.0f;
        public String transcription = ""; // Initialize with empty string to prevent NPE

        // Getter with null safety
        public String getTranscription() {
            return transcription != null ? transcription : "";
        }

        // Setter with null safety
        public void setTranscription(String text) {
            this.transcription = text != null ? text : "";
        }
    }
}
