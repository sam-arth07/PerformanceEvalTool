import logging
import os

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VideoModelTrainer:
    """
    A class to train machine learning models for video/speech analysis
    """
    
    def __init__(self):
        """Initialize the model trainer"""
        self.fluency_model = None
        self.vocabulary_model = None
        self.fluency_scaler = StandardScaler()
        self.vocabulary_scaler = StandardScaler()
    
    def generate_synthetic_data(self, n_samples=1000):
        """
        Generate synthetic speech analysis data for training
        
        Args:
            n_samples (int): Number of samples to generate
            
        Returns:
            tuple: (X, y_fluency, y_vocabulary) features and target values
        """
        logger.info(f"Generating {n_samples} synthetic speech samples")
        
        # Generate features
        np.random.seed(42)
        
        # Feature dimensions:
        # 0: Speech rate (words per minute)
        # 1: Pause frequency (pauses per minute)
        # 2: Filler word ratio (filler words / total words)
        # 3: Lexical diversity (unique words / total words)
        # 4: Average word length
        # 5: Average sentence length
        # 6: Grammatical complexity score
        
        X = np.zeros((n_samples, 7))
        
        # Speech rate (normalized from 80-180 wpm)
        X[:, 0] = np.random.normal(loc=0.6, scale=0.15, size=n_samples)
        X[:, 0] = np.clip(X[:, 0], 0.1, 1.0)
        
        # Pause frequency (normalized from 0-20 pauses per minute)
        X[:, 1] = np.random.beta(2, 5, size=n_samples)
        
        # Filler word ratio (0-0.2)
        X[:, 2] = np.random.beta(2, 15, size=n_samples)
        X[:, 2] = X[:, 2] * 0.2  # Max 20% filler words
        
        # Lexical diversity (unique words / total words, typically 0.3-0.7)
        X[:, 3] = np.random.beta(5, 5, size=n_samples) * 0.4 + 0.3
        
        # Average word length (normalized from 3-8 characters)
        X[:, 4] = np.random.normal(loc=0.6, scale=0.1, size=n_samples)
        X[:, 4] = np.clip(X[:, 4], 0.3, 0.9)
        
        # Average sentence length (normalized from 5-25 words)
        X[:, 5] = np.random.normal(loc=0.5, scale=0.15, size=n_samples)
        X[:, 5] = np.clip(X[:, 5], 0.1, 1.0)
        
        # Grammatical complexity (arbitrary score 0-1)
        X[:, 6] = np.random.beta(4, 3, size=n_samples)
        
        # Generate fluency scores
        # Higher speech rate, lower pause frequency, lower filler ratio,
        # moderate sentence length, moderate grammatical complexity = better fluency
        y_fluency = (
            0.25 * X[:, 0] +                  # Speech rate (higher is better)
            0.25 * (1.0 - X[:, 1]) +          # Pause frequency (lower is better)
            0.3 * (1.0 - X[:, 2]) +           # Filler ratio (lower is better)
            0.1 * (1.0 - np.abs(X[:, 5] - 0.6)) +  # Sentence length (moderate is best)
            0.1 * X[:, 6]                     # Grammatical complexity (higher is better)
        )
        
        # Generate vocabulary scores
        # Higher lexical diversity, longer words, more complex grammar = better vocabulary
        y_vocabulary = (
            0.4 * X[:, 3] +    # Lexical diversity
            0.4 * X[:, 4] +    # Word length
            0.2 * X[:, 6]      # Grammatical complexity
        )
        
        # Add some noise
        y_fluency += np.random.normal(0, 0.05, size=n_samples)
        y_vocabulary += np.random.normal(0, 0.05, size=n_samples)
        
        # Ensure scores are between 0 and 1
        y_fluency = np.clip(y_fluency, 0, 1)
        y_vocabulary = np.clip(y_vocabulary, 0, 1)
        
        logger.info("Synthetic speech data generation complete")
        return X, y_fluency, y_vocabulary
    
    def train_models(self, X, y_fluency, y_vocabulary):
        """
        Train models for fluency and vocabulary scoring
        
        Args:
            X: Features
            y_fluency: Fluency target values
            y_vocabulary: Vocabulary target values
            
        Returns:
            tuple: (fluency_model, vocabulary_model) Trained models
        """
        logger.info("Training speech analysis models")
        
        # Split into train and test sets
        X_train, X_test, y_fluency_train, y_fluency_test, y_vocab_train, y_vocab_test = train_test_split(
            X, y_fluency, y_vocabulary, test_size=0.2, random_state=42)
        
        # Scale features
        X_train_scaled = self.fluency_scaler.fit_transform(X_train)
        X_test_scaled = self.fluency_scaler.transform(X_test)
        
        # Train fluency model
        self.fluency_model = RandomForestRegressor(
            n_estimators=100,
            max_depth=None,
            min_samples_split=2,
            random_state=42
        )
        self.fluency_model.fit(X_train_scaled, y_fluency_train)
        
        # Evaluate fluency model
        y_fluency_pred = self.fluency_model.predict(X_test_scaled)
        fluency_mse = mean_squared_error(y_fluency_test, y_fluency_pred)
        fluency_r2 = r2_score(y_fluency_test, y_fluency_pred)
        
        logger.info(f"Fluency model - Test MSE: {fluency_mse:.4f}, R²: {fluency_r2:.4f}")
        
        # For vocabulary model, we'll reuse the same scaler
        self.vocabulary_scaler = StandardScaler().fit(X_train)
        X_train_scaled = self.vocabulary_scaler.transform(X_train)
        X_test_scaled = self.vocabulary_scaler.transform(X_test)
        
        # Train vocabulary model
        self.vocabulary_model = RandomForestRegressor(
            n_estimators=100,
            max_depth=None,
            min_samples_split=2,
            random_state=42
        )
        self.vocabulary_model.fit(X_train_scaled, y_vocab_train)
        
        # Evaluate vocabulary model
        y_vocab_pred = self.vocabulary_model.predict(X_test_scaled)
        vocab_mse = mean_squared_error(y_vocab_test, y_vocab_pred)
        vocab_r2 = r2_score(y_vocab_test, y_vocab_pred)
        
        logger.info(f"Vocabulary model - Test MSE: {vocab_mse:.4f}, R²: {vocab_r2:.4f}")
        
        return self.fluency_model, self.vocabulary_model
    
    def save_models(self, 
                   fluency_model_path='fluency_model.joblib',
                   fluency_scaler_path='fluency_scaler.joblib',
                   vocab_model_path='vocabulary_model.joblib',
                   vocab_scaler_path='vocabulary_scaler.joblib'):
        """
        Save the trained models and scalers
        
        Args:
            fluency_model_path (str): Path to save the fluency model
            fluency_scaler_path (str): Path to save the fluency scaler
            vocab_model_path (str): Path to save the vocabulary model
            vocab_scaler_path (str): Path to save the vocabulary scaler
            
        Returns:
            bool: True if successful
        """
        if self.fluency_model is None or self.vocabulary_model is None:
            logger.error("No trained models to save")
            return False
        
        try:
            joblib.dump(self.fluency_model, fluency_model_path)
            joblib.dump(self.fluency_scaler, fluency_scaler_path)
            joblib.dump(self.vocabulary_model, vocab_model_path)
            joblib.dump(self.vocabulary_scaler, vocab_scaler_path)
            
            logger.info(f"Models and scalers saved successfully")
            return True
        except Exception as e:
            logger.error(f"Error saving models: {e}")
            return False


if __name__ == "__main__":
    trainer = VideoModelTrainer()
    
    # Generate synthetic training data
    X, y_fluency, y_vocabulary = trainer.generate_synthetic_data(n_samples=5000)
    
    # Train models
    fluency_model, vocabulary_model = trainer.train_models(X, y_fluency, y_vocabulary)
    
    # Save models
    trainer.save_models()
    
    print("Video analysis model training complete!")
    print("You can now use these models in the video analyzer.")
