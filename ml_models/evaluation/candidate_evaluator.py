import numpy as np
import os
import logging
import sys
import json
from pathlib import Path
try:
    import joblib
except ImportError:
    joblib = None
    logging.warning("joblib not installed, will use fallback scoring methods")

# Add parent directory to path to import the analyzer modules
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(parent_dir)

from video_analysis.video_analyzer import VideoAnalyzer
from resume_analysis.resume_analyzer import ResumeAnalyzer

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class CandidateEvaluator:
    """
    A class that combines features from resume, CGPA, and video analysis 
    to provide a comprehensive evaluation of a candidate.
    """
    
    def __init__(self, model_path=None):
        """
        Initialize the CandidateEvaluator with required models
        
        Args:
            model_path (str, optional): Path to the trained ML model file
        """
        logger.info("Initializing CandidateEvaluator...")
        
        # Initialize component analyzers
        self.resume_analyzer = ResumeAnalyzer()
        self.video_analyzer = VideoAnalyzer()
        
        # Feature weights for different components (if not using an ML model)
        # These weights determine the importance of each factor in the final score
        self.default_weights = {
            'resume': 0.3,
            'academic': 0.2,
            'video_fluency': 0.25,
            'video_vocabulary': 0.25
        }
          # Track evaluation state
        self.current_evaluation = {
            'resume_processed': False,
            'resume_data': None,
            'video_processed': False,
            'video_data': None,
            'cgpa': None,
            'overall_result': None
        }
        
        # Load trained ML model if available
        self.model = None
        default_model_path = os.path.join(os.path.dirname(__file__), "evaluation_model.joblib")
        model_path = model_path or default_model_path
        
        if os.path.exists(model_path):
            try:
                import joblib
                self.model = joblib.load(model_path)
                logger.info(f"Loaded evaluation model from {model_path}")
            except Exception as e:
                logger.error(f"Failed to load model: {e}")
                logger.info("Will use weighted average scoring instead")
        else:
            logger.info("ML model file not found, will use weighted average scoring")
    
    def normalize_cgpa(self, cgpa, scale=10.0):
        """
        Normalize CGPA to a 0-1 scale
        
        Args:
            cgpa (float): CGPA value
            scale (float): Maximum CGPA scale (default is 10.0)
            
        Returns:
            float: Normalized CGPA score
        """
        # Ensure CGPA is within valid range
        cgpa = max(0.0, min(cgpa, scale))
        
        # Normalize to 0-1 range
        return cgpa / scale
    def evaluate_candidate(self, resume_path=None, cgpa=None, video_path=None, resume_result=None, video_result=None):
        """
        Perform a comprehensive evaluation of a candidate
        
        Args:
            resume_path (str, optional): Path to the candidate's resume file
            cgpa (float, optional): Candidate's CGPA
            video_path (str, optional): Path to the candidate's video file
            resume_result (dict, optional): Pre-analyzed resume data
            video_result (dict, optional): Pre-analyzed video data
            
        Returns:
            dict: Dictionary containing evaluation results and scores
        """
        logger.info(f"Evaluating candidate: Resume={resume_path}, CGPA={cgpa}, Video={video_path}")
        
        # Use provided analysis results or analyze components
        if resume_path and not resume_result:
            resume_result = self.resume_analyzer.analyze_resume(resume_path)
            self.current_evaluation['resume_processed'] = True
            self.current_evaluation['resume_data'] = resume_result
        elif resume_result:
            self.current_evaluation['resume_processed'] = True
            self.current_evaluation['resume_data'] = resume_result
            
        if cgpa is not None:
            self.current_evaluation['cgpa'] = cgpa
            
        if video_path and not video_result:
            video_result = self.video_analyzer.analyze_video(video_path)
            self.current_evaluation['video_processed'] = True
            self.current_evaluation['video_data'] = video_result
        elif video_result:
            self.current_evaluation['video_processed'] = True
            self.current_evaluation['video_data'] = video_result
            
        # Check if we have all required data to perform evaluation
        if not (resume_result and cgpa is not None and video_result):
            logger.warning("Incomplete evaluation data provided.")
            return {
                'error': 'Incomplete evaluation data',
                'resume_processed': self.current_evaluation['resume_processed'],
                'video_processed': self.current_evaluation['video_processed'],
                'cgpa_provided': cgpa is not None
            }
            
        resume_score = resume_result.get('score', 0.0)
        
        # Normalize CGPA
        normalized_cgpa = self.normalize_cgpa(cgpa)
        
        fluency_score = video_result.get('fluency', {}).get('score', 0.0)
        vocabulary_score = video_result.get('vocabulary', {}).get('score', 0.0)
        
        # Extract features for model input (if using an ML model)
        features = self._extract_features(resume_result, normalized_cgpa, video_result)
        
        # Calculate overall score
        if self.model:
            # Use the trained ML model to predict the score
            overall_score = self.model.predict([features])[0]
            logger.info(f"ML model evaluation score: {overall_score:.2f}")
        else:
            # Use weighted average if no model is available
            overall_score = self._calculate_weighted_score(resume_score, normalized_cgpa, 
                                                          fluency_score, vocabulary_score)
            logger.info(f"Weighted average evaluation score: {overall_score:.2f}")
        
        # Create recommendation based on overall score
        recommendation = self._generate_recommendation(overall_score, resume_score, 
                                                     normalized_cgpa, fluency_score, 
                                                     vocabulary_score)
        
        # Generate detailed feedback
        detailed_feedback = self._generate_detailed_feedback(resume_result, normalized_cgpa, 
                                                           video_result)
        
        # Prepare the final evaluation result
        evaluation_result = {
            'overall_score': overall_score,
            'component_scores': {
                'resume_score': resume_score,
                'academic_score': normalized_cgpa,
                'fluency_score': fluency_score,
                'vocabulary_score': vocabulary_score
            },
            'recommendation': recommendation,
            'detailed_feedback': detailed_feedback,
            'transcription': video_result.get('transcription', '')
        }
        
        logger.info(f"Candidate evaluation complete. Overall score: {overall_score:.2f}")
        return evaluation_result
    
    def _extract_features(self, resume_result, normalized_cgpa, video_result):
        """
        Extract features from all analyses for model input
        
        Args:
            resume_result (dict): Results from resume analysis
            normalized_cgpa (float): Normalized CGPA
            video_result (dict): Results from video analysis
            
        Returns:
            list: List of extracted features
        """
        # In a real model, we would extract many more features
        features = [
            resume_result.get('score', 0.0),
            normalized_cgpa,
            resume_result.get('skill_count', 0) / 20,  # Normalize skill count
            resume_result.get('experience_years', 0) / 10,  # Normalize experience
            video_result.get('fluency', {}).get('score', 0.0),
            video_result.get('vocabulary', {}).get('score', 0.0),
            video_result.get('fluency', {}).get('filler_word_ratio', 0.0),
            video_result.get('vocabulary', {}).get('lexical_diversity', 0.0)
        ]
        
        return features
    
    def _calculate_weighted_score(self, resume_score, academic_score, fluency_score, vocabulary_score):
        """
        Calculate weighted average score from component scores
        
        Args:
            resume_score (float): Resume analysis score
            academic_score (float): Normalized academic score (CGPA)
            fluency_score (float): Speaking fluency score
            vocabulary_score (float): Vocabulary assessment score
            
        Returns:
            float: Weighted average score
        """
        return (self.default_weights['resume'] * resume_score +
                self.default_weights['academic'] * academic_score +
                self.default_weights['video_fluency'] * fluency_score +
                self.default_weights['video_vocabulary'] * vocabulary_score)
    
    def _generate_recommendation(self, overall_score, resume_score, academic_score, 
                               fluency_score, vocabulary_score):
        """
        Generate hiring recommendation based on scores
        
        Args:
            overall_score (float): Overall evaluation score
            resume_score (float): Resume analysis score
            academic_score (float): Normalized academic score (CGPA)
            fluency_score (float): Speaking fluency score
            vocabulary_score (float): Vocabulary assessment score
            
        Returns:
            str: Recommendation text
        """
        if overall_score >= 0.8:
            return "Strong Recommendation: This candidate demonstrates excellent qualifications across all assessment areas."
        elif overall_score >= 0.7:
            return "Positive Recommendation: This candidate shows strong potential with good qualifications."
        elif overall_score >= 0.6:
            return "Moderate Recommendation: This candidate meets basic qualifications but has some areas for improvement."
        else:
            return "Not Recommended: This candidate does not meet the required qualifications for the position."
    
    def _generate_detailed_feedback(self, resume_result, normalized_cgpa, video_result):
        """
        Generate detailed feedback for the candidate
        
        Args:
            resume_result (dict): Results from resume analysis
            normalized_cgpa (float): Normalized CGPA
            video_result (dict): Results from video analysis
            
        Returns:
            dict: Detailed feedback for each component
        """
        # Resume feedback
        resume_score = resume_result.get('score', 0.0)
        if resume_score >= 0.8:
            resume_feedback = "The resume demonstrates strong relevant experience and skills."
        elif resume_score >= 0.6:
            resume_feedback = "The resume shows adequate experience but could better highlight relevant skills."
        else:
            resume_feedback = "The resume needs significant improvement to highlight relevant experience and skills."
        
        # Academic feedback
        if normalized_cgpa >= 0.8:
            academic_feedback = "Excellent academic performance demonstrates strong learning capability."
        elif normalized_cgpa >= 0.6:
            academic_feedback = "Good academic performance shows adequate educational foundation."
        else:
            academic_feedback = "Academic performance below expectations."
        
        # Video feedback - fluency
        fluency_score = video_result.get('fluency', {}).get('score', 0.0)
        if fluency_score >= 0.8:
            fluency_feedback = "Excellent speaking fluency with clear articulation and natural flow."
        elif fluency_score >= 0.6:
            fluency_feedback = "Good speaking fluency but could improve sentence structure and reduce filler words."
        else:
            fluency_feedback = "Speaking fluency needs significant improvement. Consider practice to reduce hesitations and improve flow."
        
        # Video feedback - vocabulary
        vocab_score = video_result.get('vocabulary', {}).get('score', 0.0)
        if vocab_score >= 0.8:
            vocab_feedback = "Excellent vocabulary range and appropriate use of technical terminology."
        elif vocab_score >= 0.6:
            vocab_feedback = "Good vocabulary but could benefit from expanding technical and domain-specific terms."
        else:
            vocab_feedback = "Limited vocabulary range. Consider expanding professional and technical vocabulary."
        
        return {
            'resume': resume_feedback,
            'academic': academic_feedback,
            'fluency': fluency_feedback,
            'vocabulary': vocab_feedback
        }
    
    def save_evaluation(self, evaluation_result, output_path):
        """
        Save evaluation results to a JSON file
        
        Args:
            evaluation_result (dict): The evaluation results
            output_path (str): Path to save the results
            
        Returns:
            bool: True if successful, False otherwise
        """
        try:
            with open(output_path, 'w') as f:
                json.dump(evaluation_result, f, indent=4)
            logger.info(f"Evaluation results saved to {output_path}")
            return True
        except Exception as e:
            logger.error(f"Failed to save evaluation results: {e}")
            return False


    def reset_evaluation(self):
        """
        Reset the evaluation state to start a new evaluation
        
        Returns:
            dict: The reset evaluation state
        """
        logger.info("Resetting evaluation state...")
        self.current_evaluation = {
            'resume_processed': False,
            'resume_data': None,
            'video_processed': False,
            'video_data': None,
            'cgpa': None,
            'overall_result': None
        }
        return self.current_evaluation
    
    def get_evaluation_state(self):
        """
        Get the current evaluation state
        
        Returns:
            dict: The current evaluation state
        """
        # Check if the full evaluation can be performed
        if (self.current_evaluation['resume_processed'] and 
            self.current_evaluation['video_processed'] and 
            self.current_evaluation['cgpa'] is not None and
            self.current_evaluation['overall_result'] is None):
            # We have all components but haven't done final evaluation yet
            self._perform_overall_evaluation()
            
        return self.current_evaluation
        
    def _perform_overall_evaluation(self):
        """Perform overall evaluation using available data"""
        if not all([
            self.current_evaluation['resume_processed'],
            self.current_evaluation['video_processed'],
            self.current_evaluation['cgpa'] is not None
        ]):
            logger.warning("Cannot perform overall evaluation - missing data")
            return
        
        resume_data = self.current_evaluation['resume_data']
        video_data = self.current_evaluation['video_data']
        cgpa = self.current_evaluation['cgpa']
        
        # Use the evaluate_candidate method but with already processed data
        result = self.evaluate_candidate(
            resume_path=None, 
            cgpa=cgpa, 
            video_path=None, 
            resume_result=resume_data, 
            video_result=video_data
        )
        
        self.current_evaluation['overall_result'] = result
        logger.info("Overall evaluation completed")


if __name__ == "__main__":
    # Example usage
    evaluator = CandidateEvaluator()
    
    # These would be replaced with actual file paths in the Android application
    sample_resume_path = "sample_resume.pdf"
    sample_video_path = "sample_video.mp4"
    sample_cgpa = 8.5  # On a 10-point scale
    
    if os.path.exists(sample_resume_path) and os.path.exists(sample_video_path):
        result = evaluator.evaluate_candidate(sample_resume_path, sample_cgpa, sample_video_path)
        print(f"Overall Score: {result['overall_score']:.2f}")
        print(f"Resume Score: {result['component_scores']['resume_score']:.2f}")
        print(f"Academic Score: {result['component_scores']['academic_score']:.2f}")
        
        # Now test the state tracking
        state = evaluator.get_evaluation_state()
        print(f"Evaluation state: {state['resume_processed']=}, {state['video_processed']=}")
        
        # Test resetting
        evaluator.reset_evaluation()
        print("Evaluation reset.")
        print(f"Fluency Score: {result['component_scores']['fluency_score']:.2f}")
        print(f"Vocabulary Score: {result['component_scores']['vocabulary_score']:.2f}")
        print(f"Recommendation: {result['recommendation']}")
        
        # Save results to file
        evaluator.save_evaluation(result, "evaluation_result.json")
    else:
        print("Sample files not found")
