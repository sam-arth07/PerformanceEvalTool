import cv2
import numpy as np
import librosa
import soundfile as sf
import os
import tempfile
import logging
import time
import re
import string
from collections import Counter
import joblib
import nltk
from nltk.corpus import stopwords, wordnet
from nltk.tokenize import word_tokenize, sent_tokenize

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VideoAnalyzer:
    """
    A class to analyze candidate videos and extract features related to English speaking skills.
    """
    
    def __init__(self):
        """Initialize the VideoAnalyzer with required models and resources."""
        logger.info("Initializing VideoAnalyzer...")
        
        # Download necessary NLTK resources
        try:
            nltk.data.find('tokenizers/punkt')
            nltk.data.find('corpora/stopwords')
            nltk.data.find('corpora/wordnet')
        except LookupError:
            logger.info("Downloading required NLTK resources...")
            nltk.download('punkt')
            nltk.download('stopwords')
            nltk.download('wordnet')
            logger.info("NLTK resources downloaded")
        
        # Initialize stopwords
        self.stop_words = set(stopwords.words('english'))
        
        # Load word frequency list for vocabulary assessment
        # This would be a much more comprehensive list in a production system
        self.word_frequency = {
            # Common words (high frequency)
            'the': 1, 'to': 1, 'and': 1, 'a': 1, 'in': 1, 'is': 1, 'it': 1, 
            'you': 1, 'that': 1, 'was': 1,
            
            # Medium frequency words
            'develop': 2, 'create': 2, 'design': 2, 'process': 2, 'analyze': 2,
            'experience': 2, 'professional': 2, 'implement': 2, 'solution': 2,
            'application': 2, 'system': 2, 'technology': 2,
            
            # Lower frequency / more advanced words
            'algorithm': 3, 'optimize': 3, 'architecture': 3, 'infrastructure': 3,
            'sophisticated': 3, 'implementation': 3, 'methodology': 3, 'paradigm': 3,
            'innovative': 3, 'expertise': 3, 'proficiency': 3, 'comprehensive': 3
        }
        
        logger.info("VideoAnalyzer initialization complete")
    
    def extract_audio_from_video(self, video_path, output_path=None):
        """
        Extract audio from a video file
        
        Args:
            video_path (str): Path to the video file
            output_path (str, optional): Path to save the extracted audio
            
        Returns:
            str: Path to the extracted audio file
        """
        logger.info(f"Extracting audio from video: {video_path}")
        
        if output_path is None:
            # Create a temporary file for the audio
            temp_dir = tempfile.gettempdir()
            output_path = os.path.join(temp_dir, f"audio_{int(time.time())}.wav")
        
        try:
            # Use OpenCV to extract audio
            video = cv2.VideoCapture(video_path)
            
            # In a real implementation, we would use ffmpeg for this
            # For simulation, we'll just log the process
            logger.info(f"Audio extraction would use ffmpeg in production. Simulating for now.")
            logger.info(f"Simulated audio saved to: {output_path}")
            
            # Create an empty audio file for demonstration
            if not os.path.exists(output_path):
                with open(output_path, 'wb') as f:
                    f.write(b'')
            
            return output_path
            
        except Exception as e:
            logger.error(f"Error extracting audio: {e}")
            return None
    def transcribe_audio(self, audio_path):
        """
        Transcribe speech from an audio file to text
        
        Args:
            audio_path (str): Path to the audio file
            
        Returns:
            str: Transcribed text
        """
        logger.info(f"Transcribing audio: {audio_path}")
        
        try:
            # Try to use an actual speech recognition model
            import speech_recognition as sr
            
            # Initialize recognizer
            r = sr.Recognizer()
            
            # Check if the file exists
            if not os.path.exists(audio_path):
                logger.error(f"Audio file not found: {audio_path}")
                raise FileNotFoundError(f"Audio file not found: {audio_path}")
                
            # Check if this is a sample/test run without audio
            if os.path.getsize(audio_path) < 1000:
                logger.warning("Empty or very small audio file, using sample transcription")
                raise ValueError("Audio file too small for transcription")
            
            # Convert video audio to proper format if needed
            # In a production setting, we'd use ffmpeg for this conversion
            
            # Perform speech recognition
            with sr.AudioFile(audio_path) as source:
                audio_data = r.record(source)
                text = r.recognize_google(audio_data)  # Using Google's API
                logger.info("Audio transcription complete using speech recognition")
                return text
                
        except ImportError:
            logger.warning("SpeechRecognition package not installed, using mock transcription")
        except (FileNotFoundError, ValueError) as e:
            logger.warning(f"Issue with audio file: {e}")
        except Exception as e:
            logger.error(f"Error in transcription: {e}")
            
        # Fall back to mock transcription if there's an error
        # In a production system, we would handle this differently
        mock_transcription = """
        Hello, my name is John Smith. I am very excited about this job opportunity at your company.
        I have been working as a software developer for the past three years. During this time, 
        I have gained significant experience in developing mobile applications using Java and Kotlin.
        I believe my technical skills and experience make me a strong candidate for this position.
        I am particularly interested in this role because it aligns with my career goals and offers
        an opportunity to work on innovative projects. I am confident that I can contribute effectively
        to your team and help achieve the company's objectives.
        Thank you for considering my application. I look forward to discussing my qualifications further.
        """
        
        logger.info("Using mock transcription data")
        return mock_transcription.strip()
    def extract_speech_features(self, transcription):
        """
        Extract features from transcribed speech for ML models
        
        Args:
            transcription (str): Transcribed speech text
            
        Returns:
            np.array: Feature vector for ML models
        """
        # Tokenize text
        sentences = sent_tokenize(transcription)
        words = word_tokenize(transcription)
        words_lower = [w.lower() for w in words if w.isalpha()]
        
        # Calculate basic metrics
        total_sentences = len(sentences)
        total_words = len(words_lower)
        
        if total_sentences == 0 or total_words == 0:
            logger.warning("Empty transcription or no valid sentences/words detected")
            return np.zeros(7)
            
        # Feature 0: Speech rate (approximated by words per sentence)
        speech_rate = total_words / total_sentences
        norm_speech_rate = min(speech_rate / 25, 1.0)  # Normalize assuming max 25 words per sentence
        
        # Feature 1: Pause frequency (approximated by sentence count relative to word count)
        pause_freq = total_sentences / (total_words / 10)  # Pauses per 10 words
        norm_pause_freq = min(pause_freq / 2, 1.0)  # Normalize
        
        # Feature 2: Filler word ratio
        filler_words = ['um', 'uh', 'like', 'you know', 'actually', 'basically', 'literally']
        filler_count = sum(transcription.lower().count(filler) for filler in filler_words)
        filler_ratio = filler_count / total_words if total_words > 0 else 0
        
        # Feature 3: Lexical diversity
        unique_words = len(set(words_lower))
        lexical_diversity = unique_words / total_words
        
        # Feature 4: Average word length
        avg_word_length = sum(len(word) for word in words_lower) / total_words
        norm_avg_word_length = min(avg_word_length / 8, 1.0)  # Normalize
        
        # Feature 5: Average sentence length
        avg_sentence_length = total_words / total_sentences
        norm_avg_sentence_length = min(avg_sentence_length / 25, 1.0)  # Normalize
        
        # Feature 6: Grammatical complexity
        conjunctions = ['and', 'but', 'or', 'so', 'because', 'although', 'though', 'since', 'unless', 'while']
        prepositions = ['in', 'on', 'at', 'by', 'with', 'from', 'to', 'for', 'of', 'about', 'through']
        
        # Count conjunctions and prepositions as proxies for grammatical complexity
        complexity_count = sum(words_lower.count(word) for word in conjunctions + prepositions)
        complexity_score = complexity_count / total_sentences
        norm_complexity_score = min(complexity_score / 5, 1.0)  # Normalize
        
        # Create feature vector
        features = np.array([
            norm_speech_rate,
            norm_pause_freq,
            filler_ratio,
            lexical_diversity,
            norm_avg_word_length,
            norm_avg_sentence_length,
            norm_complexity_score
        ])
        
        return features
    
    def assess_fluency(self, transcription):
        """
        Assess English fluency based on the transcribed text
        
        Args:
            transcription (str): Transcribed speech text
            
        Returns:
            dict: Dictionary containing fluency metrics
        """
        logger.info("Assessing English fluency")
        
        # Extract basic metrics
        sentences = sent_tokenize(transcription)
        words = word_tokenize(transcription)
        total_sentences = len(sentences)
        total_words = len(words)
        avg_words_per_sentence = total_words / total_sentences if total_sentences > 0 else 0
        
        # Calculate filler word ratio
        filler_words = ['um', 'uh', 'like', 'you know', 'actually', 'basically', 'literally']
        filler_count = sum(transcription.lower().count(filler) for filler in filler_words)
        filler_ratio = filler_count / total_words if total_words > 0 else 0
        
        # Calculate grammatical complexity
        conjunctions = ['and', 'but', 'or', 'so', 'because', 'although', 'though', 'since', 'unless', 'while']
        conjunction_count = sum(transcription.lower().count(' ' + conj + ' ') for conj in conjunctions)
        complexity_score = conjunction_count / total_sentences if total_sentences > 0 else 0
        
        # Extract features for ML model
        features = self.extract_speech_features(transcription)
        
        try:
            # Try to use trained ML model if available
            model_path = os.path.join(os.path.dirname(__file__), 'fluency_model.joblib')
            scaler_path = os.path.join(os.path.dirname(__file__), 'fluency_scaler.joblib')
            
            if os.path.exists(model_path) and os.path.exists(scaler_path):
                # Load model and scaler
                fluency_model = joblib.load(model_path)
                fluency_scaler = joblib.load(scaler_path)
                
                # Scale features and predict
                scaled_features = fluency_scaler.transform(features.reshape(1, -1))
                fluency_score = float(fluency_model.predict(scaled_features)[0])
                logger.info(f"Used trained ML model for fluency scoring: {fluency_score:.2f}")
            else:
                # Fall back to rule-based scoring if model not available
                logger.warning("Fluency ML model not found, using rule-based scoring")
                
                # Ideal sentence length: 10-20 words
                sentence_length_score = 1.0 - min(abs(avg_words_per_sentence - 15) / 10, 1.0)
                
                # Complexity score: normalize to 0-1
                normalized_complexity = min(complexity_score / 2.0, 1.0)
                
                # Filler word penalty: 0 fillers is best
                filler_penalty = 1.0 - min(filler_ratio * 10, 1.0)
                
                # Combined fluency score
                fluency_score = (0.4 * sentence_length_score + 
                                0.4 * normalized_complexity + 
                                0.2 * filler_penalty)
                
                # Ensure the score is between 0 and 1
                fluency_score = max(0.0, min(fluency_score, 1.0))
        except Exception as e:
            logger.error(f"Error using ML model for fluency scoring: {e}")
            # Fall back to a simple score if there's an error
            fluency_score = 0.5
            
        # Create results
        fluency_metrics = {
            'score': fluency_score,
            'total_sentences': total_sentences,
            'total_words': total_words,
            'avg_words_per_sentence': avg_words_per_sentence,
            'filler_word_ratio': filler_ratio,
            'complexity_score': complexity_score
        }
        
        logger.info(f"Fluency assessment complete. Score: {fluency_score:.2f}")
        return fluency_metrics
    def assess_vocabulary(self, transcription):
        """
        Assess vocabulary richness based on the transcribed text
        
        Args:
            transcription (str): Transcribed speech text
            
        Returns:
            dict: Dictionary containing vocabulary metrics
        """
        logger.info("Assessing vocabulary richness")
        
        # Clean and tokenize text
        text = transcription.lower()
        text = re.sub(r'[^\w\s]', '', text)  # Remove punctuation
        words = word_tokenize(text)
        
        # Remove stopwords
        content_words = [word for word in words if word not in self.stop_words and len(word) > 1]
        
        # Count unique words
        total_words = len(content_words)
        unique_words = len(set(content_words))
        
        # Calculate lexical diversity (Type-Token Ratio)
        lexical_diversity = unique_words / total_words if total_words > 0 else 0
        
        # Calculate average word length
        avg_word_length = sum(len(word) for word in content_words) / total_words if total_words > 0 else 0
        
        # Assess vocabulary sophistication
        word_sophistication = []
        for word in content_words:
            # Get word sophistication level (1-3)
            level = self.word_frequency.get(word, 1)
            word_sophistication.append(level)
        
        avg_sophistication = sum(word_sophistication) / len(word_sophistication) if word_sophistication else 0
        # Normalize to 0-1 range
        normalized_sophistication = (avg_sophistication - 1) / 2  # Maps 1-3 to 0-1
        
        try:
            # Extract features for ML model - reuse the same features we extracted for fluency
            features = self.extract_speech_features(transcription)
            
            # Try to use trained ML model if available
            model_path = os.path.join(os.path.dirname(__file__), 'vocabulary_model.joblib')
            scaler_path = os.path.join(os.path.dirname(__file__), 'vocabulary_scaler.joblib')
            
            if os.path.exists(model_path) and os.path.exists(scaler_path):
                # Load model and scaler
                vocab_model = joblib.load(model_path)
                vocab_scaler = joblib.load(scaler_path)
                
                # Scale features and predict
                scaled_features = vocab_scaler.transform(features.reshape(1, -1))
                vocabulary_score = float(vocab_model.predict(scaled_features)[0])
                logger.info(f"Used trained ML model for vocabulary scoring: {vocabulary_score:.2f}")
            else:
                # Fall back to rule-based scoring if model not available
                logger.warning("Vocabulary ML model not found, using rule-based scoring")
                
                # Calculate vocabulary score using traditional metrics
                vocabulary_score = (0.4 * lexical_diversity + 
                                   0.3 * min(avg_word_length / 10, 1.0) + 
                                   0.3 * normalized_sophistication)
        except Exception as e:
            logger.error(f"Error using ML model for vocabulary scoring: {e}")
            # Fall back to a simple score calculation
            vocabulary_score = (0.4 * lexical_diversity + 
                               0.3 * min(avg_word_length / 10, 1.0) + 
                               0.3 * normalized_sophistication)
        
        # Ensure the score is between 0 and 1
        vocabulary_score = max(0.0, min(vocabulary_score, 1.0))
        
        # Create results
        vocabulary_metrics = {
            'score': vocabulary_score,
            'total_content_words': total_words,
            'unique_words': unique_words,
            'lexical_diversity': lexical_diversity,
            'avg_word_length': avg_word_length,
            'avg_sophistication': avg_sophistication
        }
        
        logger.info(f"Vocabulary assessment complete. Score: {vocabulary_score:.2f}")
        return vocabulary_metrics
    
    def analyze_video(self, video_path):
        """
        Analyze a video to assess English speaking skills
        
        Args:
            video_path (str): Path to the video file
            
        Returns:
            dict: Dictionary containing analysis results
        """
        logger.info(f"Starting video analysis: {video_path}")
        
        # Extract audio from video
        audio_path = self.extract_audio_from_video(video_path)
        if not audio_path:
            return {'error': 'Failed to extract audio from video'}
        
        # Transcribe speech to text
        transcription = self.transcribe_audio(audio_path)
        
        # Assess fluency
        fluency_metrics = self.assess_fluency(transcription)
        
        # Assess vocabulary
        vocabulary_metrics = self.assess_vocabulary(transcription)
        
        # Calculate overall speaking score
        speaking_score = (0.5 * fluency_metrics['score'] + 
                         0.5 * vocabulary_metrics['score'])
        
        # Create complete result
        result = {
            'speaking_score': speaking_score,
            'fluency': fluency_metrics,
            'vocabulary': vocabulary_metrics,
            'transcription': transcription
        }
        
        logger.info(f"Video analysis complete. Overall score: {speaking_score:.2f}")
        return result


if __name__ == "__main__":
    # Example usage
    analyzer = VideoAnalyzer()
    
    # This would be replaced with the actual video path in the Android application
    sample_video_path = "sample_video.mp4"
    
    if os.path.exists(sample_video_path):
        result = analyzer.analyze_video(sample_video_path)
        print(f"Speaking Score: {result['speaking_score']:.2f}")
        print(f"Fluency Score: {result['fluency']['score']:.2f}")
        print(f"Vocabulary Score: {result['vocabulary']['score']:.2f}")
        print(f"Transcription excerpt: {result['transcription'][:100]}...")
    else:
        print(f"Sample video file not found: {sample_video_path}")
