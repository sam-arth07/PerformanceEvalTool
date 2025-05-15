import logging
import os

import joblib
import numpy as np
import pandas as pd
# Import resume analyzer
from resume_analyzer import ResumeAnalyzer
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.model_selection import GridSearchCV, train_test_split
from sklearn.preprocessing import StandardScaler

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ResumeModelTrainer:
    """
    A class to train a machine learning model for resume scoring
    """
    
    def __init__(self):
        """Initialize the model trainer"""
        self.resume_analyzer = ResumeAnalyzer()
        self.model = None
        self.scaler = StandardScaler()
        
    def generate_synthetic_data(self, n_samples=1000):
        """
        Generate synthetic resume data for training
        
        Args:
            n_samples (int): Number of samples to generate
            
        Returns:
            tuple: (X, y) features and target values
        """
        logger.info(f"Generating {n_samples} synthetic resume samples")
        
        # Generate features
        np.random.seed(42)
        
        # Feature dimensions:
        # - Skills by category (5 categories)
        # - Total skills
        # - Experience years
        # - Education level
        # - Sections coverage
        # - Word count
        
        # Create feature matrix with some correlations to make it realistic
        X = np.zeros((n_samples, 10))
        
        # Generate technical skill counts (0-10 for each category)
        for i in range(5):
            X[:, i] = np.random.poisson(lam=3, size=n_samples)
            X[:, i] = np.clip(X[:, i], 0, 10) / 10.0  # Normalize
        
        # Total skill count
        X[:, 5] = np.sum(X[:, :5], axis=1) * 0.8  # About 80% of the sum to account for overlapping skills
        X[:, 5] = np.clip(X[:, 5], 0, 1)  # Ensure normalized
        
        # Experience years (0-1, normalized from 0-10 years)
        X[:, 6] = np.random.beta(2, 3, size=n_samples)  # Right-skewed
        
        # Education level (0: none, 0.4: associate, 0.6: bachelors, 0.8: masters, 1.0: phd)
        education_levels = [0, 0.4, 0.6, 0.8, 1.0]
        education_probs = [0.05, 0.1, 0.6, 0.2, 0.05]  # Most have bachelor's degrees
        X[:, 7] = np.random.choice(education_levels, size=n_samples, p=education_probs)
        
        # Sections coverage (ratio of sections found)
        X[:, 8] = np.random.beta(7, 3, size=n_samples)  # Left-skewed, most resumes have most sections
        
        # Word count (normalized from 0-1000 words)
        X[:, 9] = np.random.gamma(5, 0.1, size=n_samples)
        X[:, 9] = np.clip(X[:, 9], 0, 1)
        
        # Generate target values with a complex formula to simulate real-world relationship
        y = (
            0.15 * (X[:, 0] + X[:, 1] + X[:, 2] + X[:, 3] + X[:, 4]) +  # Skills balanced across categories
            0.15 * X[:, 5] +  # Total skills
            0.25 * X[:, 6] +  # Experience heavily weighted
            0.15 * X[:, 7] +  # Education
            0.15 * X[:, 8] +  # Sections
            0.15 * X[:, 9]    # Length/detail
        )
        
        # Add some noise
        y += np.random.normal(0, 0.05, size=n_samples)
        
        # Ensure scores are between 0 and 1
        y = np.clip(y, 0, 1)
        
        logger.info("Synthetic data generation complete")
        return X, y
    
    def train_model(self, X, y):
        """
        Train a model on the given data
        
        Args:
            X: Features
            y: Target values
            
        Returns:
            RandomForestRegressor: Trained model
        """
        logger.info("Training resume scoring model")
        
        # Split into train and test sets
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Define the model and hyperparameter grid
        rf = RandomForestRegressor(random_state=42)
        param_grid = {
            'n_estimators': [50, 100, 200],
            'max_depth': [None, 10, 20],
            'min_samples_split': [2, 5, 10]
        }
        
        # Perform grid search
        grid_search = GridSearchCV(
            estimator=rf,
            param_grid=param_grid,
            cv=5,
            scoring='neg_mean_squared_error',
            n_jobs=-1
        )
        
        grid_search.fit(X_train_scaled, y_train)
        
        # Get best model
        self.model = grid_search.best_estimator_
        
        # Evaluate on test set
        y_pred = self.model.predict(X_test_scaled)
        mse = mean_squared_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)
        
        logger.info(f"Model training complete")
        logger.info(f"Best parameters: {grid_search.best_params_}")
        logger.info(f"Test MSE: {mse:.4f}")
        logger.info(f"Test R²: {r2:.4f}")
        
        return self.model, self.scaler
    
    def save_model(self, model_path='resume_model.joblib', scaler_path='resume_scaler.joblib'):
        """
        Save the trained model and scaler
        
        Args:
            model_path (str): Path to save the model
            scaler_path (str): Path to save the scaler
            
        Returns:
            bool: True if successful
        """
        if self.model is None:
            logger.error("No trained model to save")
            return False
        
        try:
            joblib.dump(self.model, model_path)
            joblib.dump(self.scaler, scaler_path)
            logger.info(f"Model saved to {model_path}")
            logger.info(f"Scaler saved to {scaler_path}")
            return True
        except Exception as e:
            logger.error(f"Error saving model: {e}")
            return False


if __name__ == "__main__":
    trainer = ResumeModelTrainer()
    
    # Generate synthetic training data
    X, y = trainer.generate_synthetic_data(n_samples=5000)
    
    # Train model
    model, scaler = trainer.train_model(X, y)
    
    # Save model
    trainer.save_model()
    
    print("Resume model training complete!")
    print("You can now use this model in the resume analyzer.")
