package com.pet.evaluator.ui.resume;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResumeViewModel extends ViewModel {
    
    private final MutableLiveData<File> resumeFile = new MutableLiveData<>();
    private final MutableLiveData<Float> cgpa = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final MutableLiveData<String> processingResult = new MutableLiveData<>();
    
    public LiveData<File> getResumeFile() {
        return resumeFile;
    }
    
    public LiveData<Float> getCgpa() {
        return cgpa;
    }
    
    public LiveData<Boolean> getIsProcessing() {
        return isProcessing;
    }
    
    public LiveData<String> getProcessingResult() {
        return processingResult;
    }
    
    public void processResume(Uri fileUri, float cgpaValue, Context context) {
        isProcessing.setValue(true);
        
        // Save CGPA value
        this.cgpa.setValue(cgpaValue);
        
        // Create a temporary file from the URI
        try {
            File tempFile = createTempFileFromUri(context, fileUri);
            resumeFile.setValue(tempFile);
            
            // In a real app, this would call the Python ML model for resume analysis
            // For now, we'll simulate a delay and successful processing
            new Thread(() -> {
                try {
                    // Simulate processing time
                    Thread.sleep(2000);
                    
                    // Update UI thread
                    processingResult.postValue("Resume processed successfully");
                    isProcessing.postValue(false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    processingResult.postValue("Error processing resume");
                    isProcessing.postValue(false);
                }
            }).start();
            
        } catch (IOException e) {
            e.printStackTrace();
            processingResult.setValue("Error processing resume: " + e.getMessage());
            isProcessing.setValue(false);
        }
    }
    
    private File createTempFileFromUri(Context context, Uri uri) throws IOException {
        String fileExtension;
        String mimeType = context.getContentResolver().getType(uri);
        
        if (mimeType != null) {
            if (mimeType.equals("application/pdf")) {
                fileExtension = ".pdf";
            } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                fileExtension = ".docx";
            } else {
                fileExtension = ".tmp";
            }
        } else {
            fileExtension = ".tmp";
        }
        
        File tempFile = File.createTempFile("resume", fileExtension, context.getCacheDir());
        tempFile.deleteOnExit();
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {
            
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return tempFile;
        }
    }
}
