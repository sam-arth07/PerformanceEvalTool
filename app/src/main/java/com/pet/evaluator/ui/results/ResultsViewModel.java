package com.pet.evaluator.ui.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ResultsViewModel extends ViewModel {

    private final MutableLiveData<Float> overallScore = new MutableLiveData<>();
    
    public LiveData<Float> getOverallScore() {
        return overallScore;
    }
    
    public float calculateOverallScore(float resumeScore, float cgpa, float fluencyScore, float vocabularyScore) {
        // Calculate weighted overall score
        // 30% resume, 20% academic, 25% fluency, 25% vocabulary
        float score = (0.30f * resumeScore) + (0.20f * cgpa) + (0.25f * fluencyScore) + (0.25f * vocabularyScore);
        overallScore.setValue(score);
        return score;
    }
    
    public String generateDetailedAnalysis(float resumeScore, float cgpa, float fluencyScore, float vocabularyScore) {
        StringBuilder analysis = new StringBuilder();
        
        // Resume analysis
        if (resumeScore >= 0.8f) {
            analysis.append("The resume shows strong experience and skills relevant to the position. ");
        } else if (resumeScore >= 0.6f) {
            analysis.append("The resume shows adequate experience but could benefit from highlighting more relevant skills. ");
        } else {
            analysis.append("The resume needs improvement to better showcase relevant skills and experience. ");
        }
        
        // Academic analysis
        if (cgpa >= 0.8f) {
            analysis.append("Academic performance is excellent, demonstrating strong learning capabilities. ");
        } else if (cgpa >= 0.6f) {
            analysis.append("Academic performance is good, showing adequate educational foundation. ");
        } else {
            analysis.append("Academic performance could be stronger, consider emphasizing practical skills. ");
        }
        
        // English fluency analysis
        if (fluencyScore >= 0.8f) {
            analysis.append("English fluency is excellent with clear articulation and natural flow. ");
        } else if (fluencyScore >= 0.6f) {
            analysis.append("English fluency is good, though there is room for improvement in sentence construction. ");
        } else {
            analysis.append("English fluency needs significant improvement, consider additional practice. ");
        }
        
        // Vocabulary analysis
        if (vocabularyScore >= 0.8f) {
            analysis.append("Vocabulary usage is impressive, with appropriate technical terminology. ");
        } else if (vocabularyScore >= 0.6f) {
            analysis.append("Vocabulary is adequate, but could benefit from more domain-specific terms. ");
        } else {
            analysis.append("Vocabulary is limited, expanding technical and professional vocabulary is recommended. ");
        }
        
        // Overall recommendation
        float overall = calculateOverallScore(resumeScore, cgpa, fluencyScore, vocabularyScore);
        if (overall >= 0.8f) {
            analysis.append("\n\nOverall, the candidate shows strong potential and is recommended for further consideration.");
        } else if (overall >= 0.65f) {
            analysis.append("\n\nOverall, the candidate shows promise but has some areas that require development.");
        } else {
            analysis.append("\n\nOverall, the candidate needs significant improvement in multiple areas before being considered further.");
        }
        
        return analysis.toString();
    }
}
