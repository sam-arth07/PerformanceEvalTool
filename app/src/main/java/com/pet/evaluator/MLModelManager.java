package com.pet.evaluator;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Interface for ML model API endpoints
 */
interface MLModelApi {
    @Multipart
    @POST("api/analyze_resume")
    Call<EvaluationResult> analyzeResume(
            @Part MultipartBody.Part file);

    @Multipart
    @POST("api/analyze_video")
    Call<EvaluationResult> analyzeVideo(
            @Part MultipartBody.Part file);

    @POST("api/reset_evaluation")
    Call<EvaluationResult> resetEvaluation();

    @Multipart
    @POST("api/evaluate")
    Call<EvaluationResult> evaluateCandidate(
            @Part MultipartBody.Part resume,
            @Part MultipartBody.Part video,
            @Part("cgpa") RequestBody cgpa);
}

/**
 * Manager class for interfacing with Python ML models
 */
public class MLModelManager {
    private static final String TAG = "MLModelManager";

    // API Base URL - Change this to your server URL
    private static final String BASE_URL = "http://10.0.2.2:5000/";

    private final MLModelApi mlModelApi;
    private final Context context;
    private boolean isServerAvailable = true; // Added flag to track server availability

    public MLModelManager(Context context) {
        this.context = context;

        // Configure OkHttp client with longer timeouts for ML processing
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // Configure Retrofit for API calls
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create API interface
        mlModelApi = retrofit.create(MLModelApi.class);
    }    /**
     * Analyze a resume file
     *
     * @param resumeUri URI of the resume file
     * @param callback  Callback for the result
     */
    public void analyzeResume(Uri resumeUri, EvaluationCallback callback) {
        // Special handling for mock URIs in demo mode
        if (resumeUri != null && "mock".equals(resumeUri.getScheme())) {
            Log.d(TAG, "Mock resume URI detected. Using demo mode with synthetic results.");
            
            // Create a synthetic result for demo purposes
            EvaluationResult demoResult = new EvaluationResult();
            // Set demo score between 70-90%
            float scoreValue = 70 + (float)(Math.random() * 20);
            demoResult.setScore(scoreValue / 100);
            // Set demo skill count between 8-15
            demoResult.setSkillCount((int)(8 + Math.random() * 7));
            // Set demo experience years between 2-6
            demoResult.setExperienceYears(2.0f + (float)(Math.random() * 4.0f));
            
            // Return demo result
            callback.onSuccess(demoResult);
            return;
        }
        
        try {
            // Convert URI to file
            File resumeFile = getFileFromUri(resumeUri);

            // Create request body
            String mimeType;
            try {
                mimeType = context.getContentResolver().getType(resumeUri);
                if (mimeType == null) {
                    // Default to common document MIME type if can't determine
                    mimeType = "application/pdf";
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not determine MIME type for resume URI: " + resumeUri, e);
                mimeType = "application/octet-stream";
            }

            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType),
                    resumeFile);

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", resumeFile.getName(), requestFile);

            // Log request details for debugging
            Log.d(TAG, "Sending resume analysis request for file: " + resumeFile.getName() + 
                  " (size: " + resumeFile.length() + " bytes)");

            // Make API call
            mlModelApi.analyzeResume(filePart).enqueue(new Callback<EvaluationResult>() {
                @Override
                public void onResponse(@NonNull Call<EvaluationResult> call,
                        @NonNull Response<EvaluationResult> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Resume analysis successful");
                        callback.onSuccess(response.body());
                    } else {
                        StringBuilder errorMsgBuilder = new StringBuilder(
                                "Failed to analyze resume. Server returned: ");
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyStr = response.errorBody().string();
                                errorMsgBuilder.append(errorBodyStr != null ? errorBodyStr : "no error details");
                            } catch (IOException e) {
                                errorMsgBuilder.append("unknown error (unable to read error body)");
                            }
                        } else {
                            errorMsgBuilder.append("unknown error");
                        }
                        String errorMsg = errorMsgBuilder.toString();
                        Log.e(TAG, errorMsg + " (HTTP Status: " + response.code() + ")");
                        callback.onError(errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<EvaluationResult> call, @NonNull Throwable t) {
                    // Check if it's a timeout or connection issue
                    boolean isTimeout = t instanceof java.net.SocketTimeoutException;
                    boolean isConnectError = t instanceof java.net.ConnectException;
                    
                    StringBuilder errorMsgBuilder = new StringBuilder();
                    
                    if (isTimeout) {
                        errorMsgBuilder.append("Server timeout: The ML server is taking too long to respond. ");
                        errorMsgBuilder.append("This could be due to high server load or network issues. ");
                    } else if (isConnectError) {
                        errorMsgBuilder.append("Connection error: Cannot reach the ML analysis server. ");
                        errorMsgBuilder.append("Please check your network connection or try again later. ");
                        // Mark server as unavailable for future calls
                        isServerAvailable = false;
                    } else {
                        errorMsgBuilder.append("Network error: ");
                        if (t != null && t.getMessage() != null) {
                            errorMsgBuilder.append(t.getMessage());
                        } else {
                            errorMsgBuilder.append("Unknown network error");
                        }
                    }
                    
                    String errorMsg = errorMsgBuilder.toString();
                    Log.e(TAG, "Resume analysis network error: " + errorMsg, t);
                    callback.onError(errorMsg);
                }
            });

        } catch (IOException e) {
            String errorMsg = "File error processing resume: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            callback.onError(errorMsg);
        }
    }

    /**
     * Analyze a video file
     *
     * @param videoUri URI of the video file
     * @param callback Callback for the result
     */
    public void analyzeVideo(Uri videoUri, EvaluationCallback callback) {
        // Special handling for mock URIs in demo mode
        if (videoUri != null && "mock".equals(videoUri.getScheme())) {
            Log.d(TAG, "Mock video URI detected. Using demo mode with synthetic results.");

            // Create a synthetic result for demo purposes
            EvaluationResult demoResult = new EvaluationResult();
            demoResult.setTranscription("This is a demo transcription generated for testing purposes. " +
                    "The candidate demonstrates good communication skills with appropriate technical vocabulary. " +
                    "Speech is clear and well-articulated with good cadence and minimal filler words.");

            // Set up demo fluency scores
            EvaluationResult.FluentyScores fluencyScores = new EvaluationResult.FluentyScores();
            // Set mock scores between 70-90
            float fluencyScore = 75 + (float) (Math.random() * 15);
            demoResult.setFluency(fluencyScores);

            // Set up demo vocabulary scores
            EvaluationResult.VocabularyScores vocabScores = new EvaluationResult.VocabularyScores();
            // Set mock scores between 70-90
            float vocabScore = 80 + (float) (Math.random() * 10);
            demoResult.setVocabulary(vocabScores);

            // Return the demo result
            callback.onSuccess(demoResult);
            return;
        }

        try {
            // Convert URI to file
            File videoFile = getFileFromUri(videoUri);

            // Create request body
            String mimeType = "";
            try {
                mimeType = context.getContentResolver().getType(videoUri);
                if (mimeType == null) {
                    mimeType = "video/mp4"; // Default MIME type for videos
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not determine MIME type for URI: " + videoUri + ", using default", e);
                mimeType = "video/mp4";
            }

            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType),
                    videoFile);

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file", videoFile.getName(), requestFile);

            // Make API call
            mlModelApi.analyzeVideo(filePart).enqueue(new Callback<EvaluationResult>() {
                @Override
                public void onResponse(@NonNull Call<EvaluationResult> call,
                        @NonNull Response<EvaluationResult> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        StringBuilder errorMsgBuilder = new StringBuilder("Failed to analyze video. Server returned: ");
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyStr = response.errorBody().string();
                                errorMsgBuilder.append(errorBodyStr != null ? errorBodyStr : "no error details");
                            } catch (IOException e) {
                                errorMsgBuilder.append("unknown error (unable to read error body)");
                            }
                        } else {
                            errorMsgBuilder.append("unknown error");
                        }
                        callback.onError(errorMsgBuilder.toString());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<EvaluationResult> call, @NonNull Throwable t) {
                    StringBuilder errorMsgBuilder = new StringBuilder("Network error: ");
                    if (t != null && t.getMessage() != null) {
                        errorMsgBuilder.append(t.getMessage());
                    } else {
                        errorMsgBuilder.append("Unknown network error");
                    }
                    callback.onError(errorMsgBuilder.toString());
                    Log.e(TAG, "Video analysis network error",
                            t != null ? t : new Throwable("No error details available"));
                }
            });

        } catch (IOException e) {
            // More detailed error logging to help diagnose issues
            Log.e(TAG, "Video file error with URI: " + videoUri, e);
            callback.onError("File error: " + e.getMessage());
        }
    }

    /**
     * Perform full candidate evaluation
     *
     * @param resumeUri URI of the resume file
     * @param videoUri  URI of the video file
     * @param cgpa      Candidate's CGPA
     * @param callback  Callback for the result
     */
    public void evaluateCandidate(Uri resumeUri, Uri videoUri, float cgpa, EvaluationCallback callback) {
        try {
            // Convert URIs to files
            File resumeFile = getFileFromUri(resumeUri);
            File videoFile = getFileFromUri(videoUri);

            // Create request bodies
            RequestBody resumeRequestFile = RequestBody.create(
                    MediaType.parse(context.getContentResolver().getType(resumeUri)),
                    resumeFile);

            RequestBody videoRequestFile = RequestBody.create(
                    MediaType.parse(context.getContentResolver().getType(videoUri)),
                    videoFile);

            MultipartBody.Part resumePart = MultipartBody.Part.createFormData(
                    "resume", resumeFile.getName(), resumeRequestFile);

            MultipartBody.Part videoPart = MultipartBody.Part.createFormData(
                    "video", videoFile.getName(), videoRequestFile);

            RequestBody cgpaPart = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(cgpa));

            // Make API call
            mlModelApi.evaluateCandidate(resumePart, videoPart, cgpaPart).enqueue(new Callback<EvaluationResult>() {
                @Override
                public void onResponse(@NonNull Call<EvaluationResult> call,
                        @NonNull Response<EvaluationResult> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        StringBuilder errorMsgBuilder = new StringBuilder(
                                "Failed to evaluate candidate. Server returned: ");
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyStr = response.errorBody().string();
                                errorMsgBuilder.append(errorBodyStr != null ? errorBodyStr : "no error details");
                            } catch (IOException e) {
                                errorMsgBuilder.append("unknown error (unable to read error body)");
                            }
                        } else {
                            errorMsgBuilder.append("unknown error");
                        }
                        callback.onError(errorMsgBuilder.toString());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<EvaluationResult> call, @NonNull Throwable t) {
                    StringBuilder errorMsgBuilder = new StringBuilder("Network error: ");
                    if (t != null && t.getMessage() != null) {
                        errorMsgBuilder.append(t.getMessage());
                    } else {
                        errorMsgBuilder.append("Unknown network error");
                    }
                    callback.onError(errorMsgBuilder.toString());
                    Log.e(TAG, "Evaluation network error", t != null ? t : new Throwable("No error details available"));
                }
            });

        } catch (IOException e) {
            callback.onError("File error: " + e.getMessage());
            Log.e(TAG, "Evaluation file error", e);
        }
    }

    /**
     * Helper method to convert a content URI to a File
     *
     * @param uri Content URI
     * @return File object
     * @throws IOException If file cannot be created
     */
    private File getFileFromUri(Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("URI is null");
        }

        // Special handling for mock URIs (when we're in testing/demo mode)
        if ("mock".equals(uri.getScheme())) {
            Log.d(TAG, "Using sample file for mock URI: " + uri);

            // For mock videos, use a sample video from resources or assets if available
            // Or use a simple dummy file if no sample is available
            String mockType = uri.getPath();
            if (mockType != null && mockType.contains("video")) {
                // Use a sample video from the sample_data directory if possible
                File externalDir = new File(context.getExternalFilesDir(null), "sample_videos");
                if (!externalDir.exists()) {
                    externalDir.mkdirs();
                }

                File sampleFile = new File(externalDir, "mock_video.mp4");

                // If we don't have a sample file, create an empty one for testing
                if (!sampleFile.exists()) {
                    // Create an empty file to represent the mock video
                    try (FileOutputStream out = new FileOutputStream(sampleFile)) {
                        // Just write some dummy data
                        out.write(new byte[1024]); // 1KB dummy file
                    }
                }

                return sampleFile;
            }
        }

        String fileName = getFileName(uri);
        File file = new File(context.getCacheDir(), fileName);

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }

            outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            return file;
        } finally {
            // Make sure streams are closed properly even if an exception occurs
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    /**
     * Helper method to get a file name from a URI
     *
     * @param uri Content URI
     * @return File name
     */
    private String getFileName(Uri uri) {
        if (uri == null) {
            return "unknown_file";
        }

        String result = null;
        String scheme = uri.getScheme();

        if (scheme != null && scheme.equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }

        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    result = path.substring(cut + 1);
                } else {
                    result = path;
                }
            } else {
                result = "unknown_file_" + System.currentTimeMillis();
            }
        }

        return result;
    }

    /**
     * Reset the evaluation state on the server
     *
     * @param callback Callback for handling the result
     */
    public void resetEvaluation(EvaluationCallback callback) {
        if (!isServerAvailable) {
            callback.onError("Server unavailable. Using on-device evaluation only.");
            return;
        }

        // No need for file processing, just call the API
        mlModelApi.resetEvaluation().enqueue(new Callback<EvaluationResult>() {
            @Override
            public void onResponse(Call<EvaluationResult> call, Response<EvaluationResult> response) {
                if (response != null && response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    StringBuilder errorMsgBuilder = new StringBuilder("Failed to reset evaluation: ");
                    if (response != null && response.errorBody() != null) {
                        try {
                            String errorBodyStr = response.errorBody().string();
                            errorMsgBuilder.append(errorBodyStr != null ? errorBodyStr : "no error details");
                        } catch (IOException e) {
                            errorMsgBuilder.append("Unknown error (unable to read error body)");
                        }
                    } else {
                        errorMsgBuilder.append("Unknown error");
                    }
                    String errorMsg = errorMsgBuilder.toString();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<EvaluationResult> call, Throwable t) {
                // Create a safe error message that won't cause null pointer exceptions
                StringBuilder errorMsgBuilder = new StringBuilder("Network error when resetting evaluation: ");

                if (t != null && t.getMessage() != null) {
                    errorMsgBuilder.append(t.getMessage());
                    Log.e(TAG, errorMsgBuilder.toString(), t);
                } else {
                    errorMsgBuilder.append("Unknown network error");
                    Log.e(TAG, errorMsgBuilder.toString());
                }
                // Send the final string to the callback
                callback.onError(errorMsgBuilder.toString());
            }
        });
    }

    /**
     * Check if the ML server is considered available
     * 
     * @return true if the server is available, false otherwise
     */
    public boolean isServerAvailable() {
        return isServerAvailable;
    }

    /**
     * Callback interface for ML model evaluations
     */
    public interface EvaluationCallback {
        void onSuccess(EvaluationResult result);

        void onError(String errorMessage);
    }
}
