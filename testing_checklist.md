# PET Application Testing Checklist

## Environment Setup Verification

-   [ ] ML Backend running on local machine (`http://localhost:5000`)
-   [ ] Android emulator or physical device connected
-   [ ] Application successfully installed on the device

## Basic Functionality Tests

### Startup Test

-   [ ] App launches without crashes
-   [ ] All main navigation tabs are accessible
-   [ ] UI elements render correctly

### Resume Analysis Tests

-   [ ] Can upload resume file from device storage
-   [ ] Successfully processes entry-level resume (e.g., `sample_resume_entry_3273.docx`)
-   [ ] Successfully processes mid-level resume (e.g., `sample_resume_mid_5098.docx`)
-   [ ] Successfully processes senior-level resume (e.g., `sample_resume_senior_1357.docx`)
-   [ ] Displays appropriate resume score
-   [ ] Handles invalid resume file gracefully
-   [ ] Shows loading indicator during processing

### Video Analysis Tests

-   [ ] Can upload video file from device storage
-   [ ] Optional: Can record video directly from camera
-   [ ] Successfully processes entry-level video (e.g., `sample_video_entry_3231.mp4`)
-   [ ] Successfully processes mid-level video (e.g., `sample_video_mid_3756.mp4`)
-   [ ] Successfully processes senior-level video (e.g., `sample_video_senior_1088.mp4`)
-   [ ] Displays fluency score correctly
-   [ ] Displays vocabulary score correctly
-   [ ] Handles invalid video file gracefully
-   [ ] Shows loading indicator during processing

### CGPA Input Tests

-   [ ] Can input CGPA value
-   [ ] Validates CGPA within correct range (0.0-10.0)
-   [ ] Shows error for invalid CGPA values
-   [ ] Saves CGPA value when navigating between tabs

### Final Evaluation Tests

-   [ ] Combines resume, video, and CGPA scores correctly
-   [ ] Displays comprehensive evaluation result
-   [ ] Shows component scores (resume, academic, fluency, vocabulary)
-   [ ] Provides appropriate recommendation based on scores
-   [ ] Handles missing components gracefully (e.g., if only resume is uploaded)

## Error Handling Tests

### API Connection Tests

-   [ ] Shows appropriate error when backend API is unavailable
-   [ ] Retries connection automatically
-   [ ] Falls back to on-device models when API is unavailable

### Input Validation Tests

-   [ ] Handles null inputs gracefully
-   [ ] Validates file types before uploading (PDF, DOCX for resumes; MP4, MOV for videos)
-   [ ] Shows appropriate error messages for invalid inputs

## Performance Tests

### Speed Tests

-   [ ] Resume upload and processing completes in reasonable time (<30 seconds)
-   [ ] Video upload and processing completes in reasonable time (<60 seconds)
-   [ ] Final evaluation generates quickly (<10 seconds)

### Memory Usage Tests

-   [ ] App doesn't crash under memory pressure
-   [ ] Large video files are handled without out-of-memory errors
-   [ ] ML model loading doesn't cause memory issues

## Special Case Tests

### Network Condition Tests

-   [ ] App works on weak/slow network connections
-   [ ] App handles network interruption during file upload
-   [ ] App caches results appropriately

### Device Compatibility Tests

-   [ ] Works on small screen devices
-   [ ] Works on large screen devices
-   [ ] Adapts to different screen orientations

## Final Verification

-   [ ] All previously reported bugs are fixed
-   [ ] No new bugs are introduced
-   [ ] App is stable for prolonged usage

## Test Results

| Test Category    | Pass/Fail | Notes |
| ---------------- | --------- | ----- |
| Startup          |           |       |
| Resume Analysis  |           |       |
| Video Analysis   |           |       |
| CGPA Input       |           |       |
| Final Evaluation |           |       |
| Error Handling   |           |       |
| Performance      |           |       |
| Special Cases    |           |       |

## Issues Found During Testing

1.
2.
3.

## Recommendations

1.
2.
3.
