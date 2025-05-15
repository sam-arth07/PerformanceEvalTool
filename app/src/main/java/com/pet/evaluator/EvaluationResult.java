package com.pet.evaluator;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Data class for evaluation results from ML models
 */
public class EvaluationResult {
    
    @SerializedName("overall_score")
    private float overallScore;
    
    @SerializedName("component_scores")
    private ComponentScores componentScores;
    
    @SerializedName("recommendation")
    private String recommendation;
    
    @SerializedName("detailed_feedback")
    private Map<String, String> detailedFeedback;
    
    @SerializedName("transcription")
    private String transcription;
    
    @SerializedName("score")
    private float score;
    
    @SerializedName("skills")
    private Map<String, String[]> skills;
    
    @SerializedName("skill_count")
    private int skillCount;
    
    @SerializedName("experience_years")
    private float experienceYears;
    
    @SerializedName("speaking_score")
    private float speakingScore;
    
    @SerializedName("fluency")
    private FluentyScores fluency;
    
    @SerializedName("vocabulary")
    private VocabularyScores vocabulary;
    
    @SerializedName("error")
    private String error;

    // Getters and setters
    public float getOverallScore() {
        return overallScore;
    }

    public ComponentScores getComponentScores() {
        return componentScores;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public Map<String, String> getDetailedFeedback() {
        return detailedFeedback;
    }

    public String getTranscription() {
        return transcription;
    }

    public float getScore() {
        return score;
    }

    public Map<String, String[]> getSkills() {
        return skills;
    }

    public int getSkillCount() {
        return skillCount;
    }

    public float getExperienceYears() {
        return experienceYears;
    }

    public float getSpeakingScore() {
        return speakingScore;
    }

    public FluentyScores getFluency() {
        return fluency;
    }

    public VocabularyScores getVocabulary() {
        return vocabulary;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    // Public setters for when we need to create or modify evaluation results
    public void setOverallScore(float overallScore) {
        this.overallScore = overallScore;
    }

    public void setComponentScores(ComponentScores componentScores) {
        this.componentScores = componentScores;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public void setDetailedFeedback(Map<String, String> detailedFeedback) {
        this.detailedFeedback = detailedFeedback;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public void setSkills(Map<String, String[]> skills) {
        this.skills = skills;
    }

    public void setSkillCount(int skillCount) {
        this.skillCount = skillCount;
    }

    public void setExperienceYears(float experienceYears) {
        this.experienceYears = experienceYears;
    }

    public void setFluency(FluentyScores fluency) {
        this.fluency = fluency;
    }

    public void setVocabulary(VocabularyScores vocabulary) {
        this.vocabulary = vocabulary;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Set the score for resume analysis
     * 
     * @param score The score value
     */
    public void setScore(float score) {
        this.score = score;
    }

    // Safe accessors that handle null values

    public FluentyScores getFluencySafe() {
        return fluency != null ? fluency : new FluentyScores();
    }

    public VocabularyScores getVocabularySafe() {
        return vocabulary != null ? vocabulary : new VocabularyScores();
    }

    public ComponentScores getComponentScoresSafe() {
        return componentScores != null ? componentScores : new ComponentScores();
    }

    public Map<String, String> getDetailedFeedbackSafe() {
        return detailedFeedback != null ? detailedFeedback : new java.util.HashMap<>();
    }

    public Map<String, String[]> getSkillsSafe() {
        return skills != null ? skills : new java.util.HashMap<>();
    }

    public String getTranscriptionSafe() {
        return transcription != null ? transcription : "";
    }

    public String getRecommendationSafe() {
        return recommendation != null ? recommendation : "";
    }

    /**
     * Nested class for component scores
     */
    public static class ComponentScores {
        @SerializedName("resume_score")
        private float resumeScore;
        
        @SerializedName("academic_score")
        private float academicScore;
        
        @SerializedName("fluency_score")
        private float fluencyScore;
        
        @SerializedName("vocabulary_score")
        private float vocabularyScore;

        public float getResumeScore() {
            return resumeScore;
        }

        public float getAcademicScore() {
            return academicScore;
        }

        public float getFluencyScore() {
            return fluencyScore;
        }

        public float getVocabularyScore() {
            return vocabularyScore;
        }

        // Setter methods
        public void setResumeScore(float resumeScore) {
            this.resumeScore = resumeScore;
        }

        public void setAcademicScore(float academicScore) {
            this.academicScore = academicScore;
        }

        public void setFluencyScore(float fluencyScore) {
            this.fluencyScore = fluencyScore;
        }

        public void setVocabularyScore(float vocabularyScore) {
            this.vocabularyScore = vocabularyScore;
        }
    }

    /**
     * Nested class for fluency scores
     */
    public static class FluentyScores {
        @SerializedName("score")
        private float score;
        
        @SerializedName("total_sentences")
        private int totalSentences;
        
        @SerializedName("total_words")
        private int totalWords;
        
        @SerializedName("avg_words_per_sentence")
        private float avgWordsPerSentence;
        
        @SerializedName("filler_word_ratio")
        private float fillerWordRatio;
        
        @SerializedName("complexity_score")
        private float complexityScore;

        public float getScore() {
            return score;
        }

        public int getTotalSentences() {
            return totalSentences;
        }

        public int getTotalWords() {
            return totalWords;
        }

        public float getAvgWordsPerSentence() {
            return avgWordsPerSentence;
        }

        public float getFillerWordRatio() {
            return fillerWordRatio;
        }        public float getComplexityScore() {
            return complexityScore;
        }
        
        public float getOverallScore() {
            // Default to score if available, otherwise return 75
            return score > 0 ? score : 75;
        }

        /**
         * Set the fluency score
         *
         * @param score The fluency score value
         */
        public void setScore(float score) {
            this.score = score;
        }
    }

    /**
     * Nested class for vocabulary scores
     */
    public static class VocabularyScores {
        @SerializedName("score")
        private float score;
        
        @SerializedName("total_content_words")
        private int totalContentWords;
        
        @SerializedName("unique_words")
        private int uniqueWords;
        
        @SerializedName("lexical_diversity")
        private float lexicalDiversity;
        
        @SerializedName("avg_word_length")
        private float avgWordLength;
        
        @SerializedName("avg_sophistication")
        private float avgSophistication;

        public float getScore() {
            return score;
        }

        public int getTotalContentWords() {
            return totalContentWords;
        }        public int getUniqueWords() {
            return uniqueWords;
        }
        
        public float getOverallScore() {
            // Default to score if available, otherwise return 85
            return score > 0 ? score : 85;
        }

        public float getLexicalDiversity() {
            return lexicalDiversity;
        }

        public float getAvgWordLength() {
            return avgWordLength;
        }

        public float getAvgSophistication() {
            return avgSophistication;
        }

        /**
         * Set the vocabulary score
         *
         * @param score The vocabulary score value
         */
        public void setScore(float score) {
            this.score = score;
        }
    }
}
