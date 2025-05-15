import spacy
import os
import re
import numpy as np
import pandas as pd
from pdfminer.high_level import extract_text as extract_text_pdf
from docx import Document
import logging
import joblib
from sklearn.ensemble import RandomForestRegressor
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import StandardScaler

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ResumeAnalyzer:
    """
    A class to analyze candidate resumes and extract relevant features for evaluation.
    """
    
    def __init__(self):
        """Initialize the ResumeAnalyzer with NLP models and skills database."""
        logger.info("Initializing ResumeAnalyzer...")
        try:
            # Load spaCy model for NLP tasks
            self.nlp = spacy.load("en_core_web_sm")
            logger.info("Loaded spaCy NLP model")
        except OSError:
            logger.warning("spaCy model not found. Downloading...")
            os.system("python -m spacy download en_core_web_sm")
            self.nlp = spacy.load("en_core_web_sm")
        
        # Define skills and keywords to look for
        self.technical_skills = {
            'programming': ['python', 'java', 'javascript', 'c++', 'c#', 'ruby', 'php',
                           'html', 'css', 'sql', 'nosql', 'swift', 'kotlin', 'go', 'rust'],
            'data_science': ['machine learning', 'deep learning', 'data mining', 'statistics',
                           'data analysis', 'data visualization', 'ai', 'artificial intelligence',
                           'tensorflow', 'pytorch', 'scikit-learn', 'pandas', 'numpy'],
            'web_dev': ['react', 'angular', 'vue', 'node.js', 'django', 'flask', 'express',
                      'frontend', 'backend', 'fullstack', 'responsive', 'rest api'],
            'mobile_dev': ['android', 'ios', 'react native', 'flutter', 'swift', 'kotlin',
                         'mobile application', 'app development'],
            'devops': ['docker', 'kubernetes', 'jenkins', 'ci/cd', 'aws', 'azure', 'gcp',
                     'cloud', 'git', 'github', 'gitlab', 'devops']
        }
        
        # Important sections to look for in a resume
        self.sections = ['education', 'experience', 'skills', 'projects', 'certifications', 
                        'publications', 'awards', 'achievements']
    
    def read_resume(self, file_path):
        """
        Extract text from resume file (PDF or DOCX)
        
        Args:
            file_path (str): Path to the resume file
            
        Returns:
            str: Extracted text from the resume
        """
        logger.info(f"Reading resume from: {file_path}")
        if file_path.endswith('.pdf'):
            try:
                text = extract_text_pdf(file_path)
                return text
            except Exception as e:
                logger.error(f"Error extracting PDF text: {e}")
                return ""
        elif file_path.endswith('.docx'):
            try:
                doc = Document(file_path)
                text = '\n'.join([para.text for para in doc.paragraphs])
                return text
            except Exception as e:
                logger.error(f"Error extracting DOCX text: {e}")
                return ""
        else:
            logger.error(f"Unsupported file format: {file_path}")
            return ""
    
    def extract_sections(self, text):
        """
        Extract different sections from the resume text
        
        Args:
            text (str): Resume text
            
        Returns:
            dict: Dictionary containing different sections of the resume
        """
        # Simple section extraction based on common headers
        sections = {}
        
        # Convert text to lowercase for easier matching
        lower_text = text.lower()
        
        # Find potential section headers
        for section in self.sections:
            # Look for section headers (e.g., "EDUCATION", "Education:", etc.)
            patterns = [
                rf'\b{section}\b',
                rf'\b{section.upper()}\b',
                rf'\b{section.title()}\b',
                rf'\b{section.title()}:',
                rf'\b{section.upper()}:'
            ]
            
            for pattern in patterns:
                matches = re.finditer(pattern, lower_text)
                for match in matches:
                    start_idx = match.end()
                    # Look for the next section header or end of text
                    end_idx = len(text)
                    for next_section in self.sections:
                        next_patterns = [
                            rf'\b{next_section}\b',
                            rf'\b{next_section.upper()}\b',
                            rf'\b{next_section.title()}\b',
                            rf'\b{next_section.title()}:',
                            rf'\b{next_section.upper()}:'
                        ]
                        
                        for next_pattern in next_patterns:
                            next_match = re.search(next_pattern, lower_text[start_idx:])
                            if next_match and next_section != section:
                                if start_idx + next_match.start() < end_idx:
                                    end_idx = start_idx + next_match.start()
                    
                    section_content = text[start_idx:end_idx].strip()
                    if section not in sections:  # Only add the section once
                        sections[section] = section_content
        
        return sections
    
    def extract_skills(self, text):
        """
        Extract skills mentioned in the resume text
        
        Args:
            text (str): Resume text
            
        Returns:
            dict: Dictionary containing skill counts by category
        """
        text_lower = text.lower()
        skills = {category: [] for category in self.technical_skills}
        
        for category, skill_list in self.technical_skills.items():
            for skill in skill_list:
                if re.search(rf'\b{re.escape(skill)}\b', text_lower):
                    skills[category].append(skill)
        
        return skills
    
    def calculate_experience_years(self, text):
        """
        Estimate years of experience from resume text
        
        Args:
            text (str): Resume text
            
        Returns:
            float: Estimated years of experience
        """
        # Look for patterns like "X years of experience" or date ranges
        experience_patterns = [
            r'(\d+)[\+]?\s+years?(?:\s+of)?\s+(?:experience|work)',
            r'(\d{4})\s*-\s*(?:present|current|now|\d{4})',
            r'(\d{4})\s*to\s*(?:present|current|now|\d{4})'
        ]
        
        current_year = 2025  # Using a fixed current year for the example
        years = []
        
        for pattern in experience_patterns:
            matches = re.finditer(pattern, text.lower())
            for match in matches:
                if 'years' in pattern:
                    # Direct mention of years of experience
                    try:
                        years.append(float(match.group(1)))
                    except (ValueError, IndexError):
                        continue
                else:
                    # Date range extraction
                    try:
                        start_year = int(match.group(1))
                        if 'present' in match.group(0) or 'current' in match.group(0) or 'now' in match.group(0):
                            end_year = current_year
                        else:
                            # Try to extract the end year from the match
                            end_year_match = re.search(r'(\d{4})$', match.group(0))
                            if end_year_match:
                                end_year = int(end_year_match.group(1))
                            else:
                                continue
                        
                        if start_year <= end_year and start_year > 1950:  # Sanity check for years
                            years.append(end_year - start_year)
                    except (ValueError, IndexError):
                        continue
        
        if years:
            # Return the maximum years of experience found
            return max(years)
        else:
            return 0
    
    def extract_education(self, text):
        """
        Extract education details from resume text
        
        Args:
            text (str): Resume text
            
        Returns:
            dict: Dictionary containing education details
        """
        education = {
            'degree_level': None,
            'degree_field': None,
            'institution': None
        }
        
        # Look for degree levels
        degree_patterns = {
            'phd': r'ph\.?d\.?|doctor\s+of\s+philosophy',
            'masters': r'master\'?s?|m\.?s\.?|m\.?eng\.?|m\.?tech\.?|m\.?b\.?a\.?',
            'bachelors': r'bachelor\'?s?|b\.?s\.?|b\.?tech\.?|b\.?e\.?|b\.?a\.?',
            'associate': r'associate\'?s?|a\.?s\.?'
        }
        
        text_lower = text.lower()
        
        # Extract degree level
        for level, pattern in degree_patterns.items():
            if re.search(pattern, text_lower):
                education['degree_level'] = level
                break
        
        # Extract field of study (common fields)
        fields = [
            'computer science', 'engineering', 'information technology', 'data science',
            'business administration', 'mathematics', 'physics', 'economics', 'finance'
        ]
        
        for field in fields:
            if field in text_lower:
                education['degree_field'] = field
                break
        
        return education
    def extract_ml_features(self, text, sections, skills, experience_years, education):
        """
        Extract features for the machine learning model
        
        Args:
            text (str): Resume text
            sections (dict): Extracted sections
            skills (dict): Extracted skills
            experience_years (float): Years of experience
            education (dict): Education details
            
        Returns:
            np.array: Feature vector for ML model
        """
        # Feature 1: Skill count by category
        skill_counts = [len(skills[category]) for category in self.technical_skills]
        skill_count_total = sum(skill_counts)
        
        # Feature 2: Normalized experience years (0-1)
        norm_experience = min(experience_years / 10.0, 1.0)  # Cap at 10 years
        
        # Feature 3: Education level encoded
        education_level = 0
        if education['degree_level'] == 'phd':
            education_level = 1.0
        elif education['degree_level'] == 'masters':
            education_level = 0.8
        elif education['degree_level'] == 'bachelors':
            education_level = 0.6
        elif education['degree_level'] == 'associate':
            education_level = 0.4
            
        # Feature 4: Sections coverage (ratio of found sections to all possible sections)
        sections_coverage = len(sections) / len(self.sections)
        
        # Feature 5: Word count (proxy for detail level)
        word_count = len(text.split())
        norm_word_count = min(word_count / 1000.0, 1.0)  # Normalize to 0-1
        
        # Create feature vector
        features = np.array([
            *[count / 10.0 for count in skill_counts],  # Normalize skill counts
            skill_count_total / 20.0,  # Normalize total skill count
            norm_experience,
            education_level,
            sections_coverage,
            norm_word_count
        ])
        
        return features
        
    def analyze_resume(self, file_path):
        """
        Analyze a resume and extract key features
        
        Args:
            file_path (str): Path to resume file
            
        Returns:
            dict: Dictionary containing extracted features and score
        """
        logger.info(f"Analyzing resume: {file_path}")
        
        # Read resume text
        text = self.read_resume(file_path)
        if not text:
            return {'score': 0.0, 'error': 'Failed to extract text from resume'}
        
        # Parse the document with spaCy for NLP analysis
        doc = self.nlp(text)
        
        # Extract sections
        sections = self.extract_sections(text)
        
        # Extract skills
        skills = self.extract_skills(text)
        skill_count = sum(len(skills[category]) for category in skills)
        
        # Calculate experience
        experience_years = self.calculate_experience_years(text)
        
        # Extract education
        education = self.extract_education(text)
        
        # Extract features for ML model
        features = self.extract_ml_features(text, sections, skills, experience_years, education)
        
        try:
            # Use ML model for prediction if available
            from sklearn.ensemble import RandomForestRegressor
            import joblib
            model_path = os.path.join(os.path.dirname(__file__), 'resume_model.joblib')
            
            if os.path.exists(model_path):
                # Load and use the trained model
                model = joblib.load(model_path)
                score = float(model.predict([features])[0])
                logger.info(f"Used trained ML model for resume scoring: {score:.2f}")
            else:
                # Fall back to rule-based scoring if model not available
                logger.warning("ML model not found, using rule-based scoring")
                
                # Skills score (0-40 points)
                max_skills = 20
                skills_score = min(skill_count / max_skills, 1.0) * 40
                
                # Experience score (0-30 points)
                max_experience = 10  # Assume 10 years is the maximum relevant experience
                experience_score = min(experience_years / max_experience, 1.0) * 30
                
                # Education score (0-30 points)
                education_score = 0
                if education['degree_level'] == 'phd':
                    education_score = 30
                elif education['degree_level'] == 'masters':
                    education_score = 25
                elif education['degree_level'] == 'bachelors':
                    education_score = 20
                elif education['degree_level'] == 'associate':
                    education_score = 15
                
                # Total score (0-100 points), converted to 0-1 range
                score = (skills_score + experience_score + education_score) / 100
        except Exception as e:
            logger.error(f"Error using ML model for resume scoring: {e}")
            # Fall back to basic score if there's an error
            score = min(skill_count / 20.0 + experience_years / 10.0 + (0.6 if education['degree_level'] else 0), 1.0)
          # Prepare the result
        result = {
            'score': score,
            'skills': skills,
            'skill_count': skill_count,
            'experience_years': experience_years,
            'education': education,
            'text_length': len(text),
            'sections_found': list(sections.keys()),
        }
        logger.info(f"Resume analysis complete. Score: {score:.2f}")
        return result


if __name__ == "__main__":
    # Example usage
    analyzer = ResumeAnalyzer()
    
    # This would be replaced with actual file paths in the Android application
    sample_resume_path = "sample_resume.pdf"
    
    if os.path.exists(sample_resume_path):
        result = analyzer.analyze_resume(sample_resume_path)
        print(f"Resume Score: {result['score']:.2f}")
        print(f"Skills found: {result['skill_count']}")
        for category, category_skills in result['skills'].items():
            if category_skills:
                print(f"  {category.replace('_', ' ').title()}: {', '.join(category_skills)}")
        print(f"Experience: {result['experience_years']} years")
        print(f"Education: {result['education']['degree_level']} in {result['education']['degree_field']}")
    else:
        print(f"Sample resume file not found: {sample_resume_path}")
