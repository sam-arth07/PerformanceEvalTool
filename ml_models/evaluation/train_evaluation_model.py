import json
import logging
import os
import sys
from pathlib import Path

import numpy as np
import pandas as pd

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

try:
    import joblib
    from sklearn.ensemble import RandomForestRegressor
    from sklearn.metrics import mean_squared_error, r2_score
    from sklearn.model_selection import GridSearchCV, train_test_split
    from sklearn.preprocessing import StandardScaler
    sklearn_available = True
except ImportError:
    logger.warning("Required ML libraries not available. Install sklearn and joblib.")
    sklearn_available = False

class EvaluationModelTrainer:
    """
    Trains a machine learning model for overall candidate evaluation that combines
    resume, academic, and video interview scores.
    """
    
    def __init__(self):
        """Initialize the trainer"""
        self.model = None
        self.scaler = StandardScaler()
        
        if not sklearn_available:
            logger.error("scikit-learn and joblib are required for model training")
            
    def generate_synthetic_data(self, n_samples=5000):
        """
        Generate synthetic data for training the overall evaluation model
        
        Args:
            n_samples: Number of samples to generate
            
        Returns:
            X: Features (resume_score, academic_score, fluency_score, vocabulary_score)
            y: Target overall score
        """
        logger.info(f"Generating {n_samples} synthetic evaluation examples")
        
        np.random.seed(42)
        
        # Generate component scores
        resume_scores = np.random.beta(6, 3, n_samples)  # Right-skewed
        academic_scores = np.random.beta(5, 2, n_samples)  # Right-skewed, slightly higher
        fluency_scores = np.random.beta(4, 3, n_samples)  # Centered around 0.6
        vocabulary_scores = np.random.beta(4, 3, n_samples)  # Similar to fluency
        
        # Add correlations between scores (people with good resumes often speak well too)
        base_quality = np.random.beta(3, 2, n_samples)  # Underlying candidate quality
        
        resume_scores = (0.7 * resume_scores + 0.3 * base_quality)
        resume_scores = np.clip(resume_scores, 0, 1)
        
        academic_scores = (0.8 * academic_scores + 0.2 * base_quality)
        academic_scores = np.clip(academic_scores, 0, 1)
        
        fluency_scores = (0.6 * fluency_scores + 0.4 * base_quality)
        fluency_scores = np.clip(fluency_scores, 0, 1)
        
        vocabulary_scores = (0.6 * vocabulary_scores + 0.4 * base_quality)
        vocabulary_scores = np.clip(vocabulary_scores, 0, 1)
        
        # Create feature matrix
        X = np.column_stack((resume_scores, academic_scores, fluency_scores, vocabulary_scores))
        
        # Generate overall scores with configurable weights
        weights = np.array([0.3, 0.2, 0.25, 0.25])  # Component weights
        y = np.dot(X, weights)
        
        # Add some noise
        y += np.random.normal(0, 0.05, n_samples)
        y = np.clip(y, 0, 1)  # Ensure in valid range
        
        logger.info("Synthetic data generation complete")
        return X, y
        
    def train_model(self, X, y):
        """
        Train an ML model to predict overall evaluation score
        
        Args:
            X: Features (resume_score, academic_score, fluency_score, vocabulary_score)
            y: Target overall score
            
        Returns:
            Trained model
        """
        if not sklearn_available:
            logger.error("Required libraries not available")
            return None
            
        logger.info("Training evaluation model")
        
        # Split into train/test sets
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Define model
        rf = RandomForestRegressor(random_state=42)
        
        # Parameter grid for tuning
        param_grid = {
            'n_estimators': [50, 100],
            'max_depth': [None, 10],
            'min_samples_split': [2, 5]
        }
        
        # Grid search
        grid_search = GridSearchCV(
            estimator=rf,
            param_grid=param_grid,
            cv=3,
            scoring='neg_mean_squared_error'
        )
        
        grid_search.fit(X_train_scaled, y_train)
        self.model = grid_search.best_estimator_
        
        # Evaluate
        y_pred = self.model.predict(X_test_scaled)
        mse = mean_squared_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)
        
        logger.info(f"Model training complete")
        logger.info(f"Test MSE: {mse:.4f}")
        logger.info(f"Test R²: {r2:.4f}")
        logger.info(f"Best parameters: {grid_search.best_params_}")
        
        return self.model
    
    def save_model(self, output_dir=None):
        """Save the trained model"""
        if self.model is None:
            logger.error("No model to save")
            return False
            
        if output_dir is None:
            output_dir = os.path.dirname(os.path.abspath(__file__))
            
        try:
            model_path = os.path.join(output_dir, 'evaluation_model.joblib')
            scaler_path = os.path.join(output_dir, 'evaluation_scaler.joblib')
            
            joblib.dump(self.model, model_path)
            joblib.dump(self.scaler, scaler_path)
            
            logger.info(f"Model saved to {model_path}")
            logger.info(f"Scaler saved to {scaler_path}")
            return True
        except Exception as e:
            logger.error(f"Error saving model: {e}")
            return False
            
    def save_tflite(self, output_dir=None):
        """Convert and save the model in TFLite format"""
        if self.model is None:
            logger.error("No model to save")
            return False
            
        if output_dir is None:
            output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'tflite_models')
            os.makedirs(output_dir, exist_ok=True)
            
        try:
            # For demonstration, we'll use a special function to convert sklearn to TFLite
            # In a real implementation, you would either:
            # 1. Retrain the model in TensorFlow and then convert, or
            # 2. Use specific converters like sklearn-to-tf or onnx
            
            logger.info("Converting model to TFLite format (simulated)")
            
            # Create a representative metadata file
            metadata = {
                "input_shape": [4],
                "input_dtype": "float32", 
                "output_shape": [1],
                "output_dtype": "float32",
                "input_names": ["resume_score", "academic_score", "fluency_score", "vocabulary_score"],
                "output_names": ["overall_score"],
                "version": "1.0"
            }
            
            metadata_path = os.path.join(output_dir, "evaluation_model_metadata.json")
            with open(metadata_path, 'w') as f:
                json.dump(metadata, f, indent=2)
                
            logger.info(f"Model metadata saved to {metadata_path}")
            
            # In a real implementation, we would convert to TFLite here
            return True
            
        except Exception as e:
            logger.error(f"Error converting model to TFLite: {e}")
            return False

if __name__ == "__main__":
    trainer = EvaluationModelTrainer()
    
    if sklearn_available:
        # Generate training data
        X, y = trainer.generate_synthetic_data(n_samples=5000)
        
        # Train model
        model = trainer.train_model(X, y)
        
        # Save the model
        trainer.save_model()
        
        # Save TFLite version (simulated)
        trainer.save_tflite()
        
        print("Evaluation model training complete!")
    else:
        print("Error: Required libraries not available.")
        print("Install scikit-learn and joblib to train the model.")
