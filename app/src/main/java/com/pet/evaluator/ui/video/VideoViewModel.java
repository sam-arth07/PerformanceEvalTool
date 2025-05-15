package com.pet.evaluator.ui.video;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VideoViewModel extends ViewModel {
    
    private final MutableLiveData<File> videoFile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<String> processingResult = new MutableLiveData<>();
    
    // Results from video analysis
    private final MutableLiveData<Float> fluencyScore = new MutableLiveData<>();
    private final MutableLiveData<Float> vocabularyScore = new MutableLiveData<>();
    private final MutableLiveData<String> transcription = new MutableLiveData<>();
    
    public LiveData<File> getVideoFile() {
        return videoFile;
    }
    
    public LiveData<Boolean> getIsProcessing() {
        return isProcessing;
    }
    
    public LiveData<String> getProcessingResult() {
        return processingResult;
    }
    
    public LiveData<Float> getFluencyScore() {
        return fluencyScore;
    }
    
    public LiveData<Float> getVocabularyScore() {
        return vocabularyScore;
    }
    
    public LiveData<String> getTranscription() {
        return transcription;
    }
    
    public void processVideo(Uri fileUri, Context context) {
        isProcessing.setValue(true);
        
        // In a real app, this is where you'd call your Python ML model for video analysis
        // For now, we'll simulate processing with a delay
        
        new Thread(() -> {
            try {
                // If we have a real video file, create a temp file from URI
                if (fileUri != null) {
                    try {
                        File tempFile = createTempFileFromUri(context, fileUri);
                        videoFile.postValue(tempFile);
                        Log.d("VideoViewModel", "Temp video file created: " + tempFile.getAbsolutePath());
                    } catch (IOException e) {
                        Log.e("VideoViewModel", "Error creating temp file", e);
                        processingResult.postValue("Error processing video: " + e.getMessage());
                        isProcessing.postValue(false);
                        return;
                    }
                }
                
                // Simulate processing time
                Thread.sleep(3000);
                
                // Generate mock results (would come from ML model in real app)
                float mockFluency = 0.75f; // 75%
                float mockVocabulary = 0.85f; // 85%
                String mockTranscript = "Hello, my name is John Smith. I am applying for the software engineer position. " +
                        "I have five years of experience in developing Android applications. " +
                        "I believe my skills in Java, Kotlin, and machine learning make me a strong candidate.";
                
                // Update the LiveData objects with results
                fluencyScore.postValue(mockFluency);
                vocabularyScore.postValue(mockVocabulary);
                transcription.postValue(mockTranscript);
                
                // Complete processing
                processingResult.postValue("Video analyzed successfully");
                isProcessing.postValue(false);
                
            } catch (InterruptedException e) {
                Log.e("VideoViewModel", "Processing interrupted", e);
                processingResult.postValue("Error processing video");
                isProcessing.postValue(false);
            }
        }).start();
    }
    
    private File createTempFileFromUri(Context context, Uri uri) throws IOException {
        File tempFile = File.createTempFile("video", ".mp4", context.getCacheDir());
        tempFile.deleteOnExit();
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return tempFile;
        }
    }
}
