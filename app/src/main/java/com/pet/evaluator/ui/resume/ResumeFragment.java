package com.pet.evaluator.ui.resume;

import android.app.Activity;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.pet.evaluator.R;
import com.pet.evaluator.SharedViewModel;

public class ResumeFragment extends Fragment {
    
    private static final int PICK_PDF_FILE = 1;
    
    private SharedViewModel sharedViewModel;
    private TextView selectedFileTextView;
    private TextInputEditText cgpaEditText;
    private Button uploadButton;
    private Button saveButton;
    private Button continueButton;
    private ProgressBar progressBar;
    private Uri selectedFileUri;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Use the activity scope for ViewModel to share data across fragments
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        return inflater.inflate(R.layout.fragment_resume, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components
        selectedFileTextView = view.findViewById(R.id.text_selected_file);
        cgpaEditText = view.findViewById(R.id.edit_cgpa);
        uploadButton = view.findViewById(R.id.button_upload_resume);
        saveButton = view.findViewById(R.id.button_save_resume_info);
        continueButton = view.findViewById(R.id.button_continue_to_video);
        progressBar = view.findViewById(R.id.progress_resume);
        
        // Set click listeners
        uploadButton.setOnClickListener(v -> openFilePicker());
        saveButton.setOnClickListener(v -> saveResumeInfo());
        continueButton.setOnClickListener(v -> 
                Navigation.findNavController(requireView()).navigate(R.id.navigation_video));
        
        // Observe shared view model data
        sharedViewModel.getIsProcessing().observe(getViewLifecycleOwner(), isProcessing -> {
            if (Boolean.TRUE.equals(isProcessing)) {
                progressBar.setVisibility(View.VISIBLE);
                saveButton.setEnabled(false);
                uploadButton.setEnabled(false);
                continueButton.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                uploadButton.setEnabled(true);
                continueButton.setEnabled(true);
            }
        });
        
        sharedViewModel.getIsResumeProcessed().observe(getViewLifecycleOwner(), isProcessed -> {
            if (Boolean.TRUE.equals(isProcessed)) {
                continueButton.setVisibility(View.VISIBLE);
            } else {
                continueButton.setVisibility(View.GONE);
            }
        });
        
        // Check if we already have CGPA data
        sharedViewModel.getCgpa().observe(getViewLifecycleOwner(), cgpa -> {
            if (cgpa != null) {
                cgpaEditText.setText(String.valueOf(cgpa));
            }
        });
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        
        // Also accept docx files
        String[] mimeTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        startActivityForResult(intent, PICK_PDF_FILE);
    }
    
    private void saveResumeInfo() {
        String cgpaText = cgpaEditText.getText().toString();
        
        if (cgpaText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter CGPA", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            float cgpa = Float.parseFloat(cgpaText);
            if (cgpa < 0 || cgpa > 10) {
                Toast.makeText(getContext(), R.string.invalid_cgpa, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedFileUri == null) {
                Toast.makeText(getContext(), "Please upload a resume file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Process resume using the shared view model
            sharedViewModel.processResume(selectedFileUri, cgpa);
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.invalid_cgpa, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedFileUri = data.getData();
                
                // Get file name from URI
                String fileName = "Selected File";
                
                // Try to get the actual file name
                String scheme = selectedFileUri.getScheme();
                if (scheme.equals("content")) {
                    try {
                        String[] projection = {android.provider.MediaStore.MediaColumns.DISPLAY_NAME};
                        android.database.Cursor cursor = requireActivity().getContentResolver().query(
                                selectedFileUri, projection, null, null, null);
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME);
                            fileName = cursor.getString(columnIndex);
                            cursor.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (scheme.equals("file")) {
                    fileName = selectedFileUri.getLastPathSegment();
                }
                
                selectedFileTextView.setText(fileName);
                Toast.makeText(getContext(), R.string.resume_uploaded_success, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
