import json
import logging
import os
# Import ML models
import sys
import tempfile
import time

from flask import Flask, jsonify, request
from werkzeug.utils import secure_filename

# Add parent directory to path to help with imports
current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(current_dir)
sys.path.insert(0, parent_dir)
sys.path.insert(0, current_dir)

try:
    from ml_models.evaluation.candidate_evaluator import CandidateEvaluator
    from ml_models.resume_analysis.resume_analyzer import ResumeAnalyzer
    from ml_models.video_analysis.video_analyzer import VideoAnalyzer
except ImportError:
    # Local development fallback
    from evaluation.candidate_evaluator import CandidateEvaluator
    from resume_analysis.resume_analyzer import ResumeAnalyzer
    from video_analysis.video_analyzer import VideoAnalyzer

# Set up logging
logging.basicConfig(level=logging.INFO, 
                    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Initialize the ML models
evaluator = CandidateEvaluator()
resume_analyzer = ResumeAnalyzer()
video_analyzer = VideoAnalyzer()

# Configure upload folder
UPLOAD_FOLDER = tempfile.gettempdir()
ALLOWED_EXTENSIONS = {'pdf', 'docx', 'mp4', 'mov', 'avi'}

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = 32 * 1024 * 1024  # 32MB max file size


def allowed_file(filename):
    """Check if the file extension is allowed"""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'healthy', 'version': '1.0.0'})


@app.route('/api/analyze_resume', methods=['POST'])
def analyze_resume():
    """API endpoint to analyze a resume"""
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if file and allowed_file(file.filename):
        # Save the uploaded file
        filename = secure_filename(file.filename)
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], 
                                f"{int(time.time())}_{filename}")
        file.save(file_path)
        
        try:
            # Analyze the resume
            result = resume_analyzer.analyze_resume(file_path)
            
            # Clean up
            os.remove(file_path)
            
            return jsonify(result)
        
        except Exception as e:
            logger.error(f"Error analyzing resume: {e}")
            return jsonify({'error': str(e)}), 500
    
    return jsonify({'error': 'File type not allowed'}), 400


@app.route('/api/analyze_video', methods=['POST'])
def analyze_video():
    """API endpoint to analyze a video"""
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400
    
    if file and allowed_file(file.filename):
        # Save the uploaded file
        filename = secure_filename(file.filename)
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], 
                                f"{int(time.time())}_{filename}")
        file.save(file_path)
        
        try:
            # Analyze the video
            result = video_analyzer.analyze_video(file_path)
            
            # Clean up
            os.remove(file_path)
            
            return jsonify(result)
        
        except Exception as e:
            logger.error(f"Error analyzing video: {e}")
            return jsonify({'error': str(e)}), 500
    
    return jsonify({'error': 'File type not allowed'}), 400


@app.route('/api/evaluate', methods=['POST'])
def evaluate_candidate():
    """API endpoint for full candidate evaluation"""
    # Check if all required files are present
    if 'resume' not in request.files:
        return jsonify({'error': 'Resume file missing'}), 400
    
    if 'video' not in request.files:
        return jsonify({'error': 'Video file missing'}), 400
    
    if 'cgpa' not in request.form:
        return jsonify({'error': 'CGPA value missing'}), 400
    
    resume_file = request.files['resume']
    video_file = request.files['video']
    
    try:
        cgpa = float(request.form['cgpa'])
    except ValueError:
        return jsonify({'error': 'Invalid CGPA value'}), 400
    
    # Validate files
    if resume_file.filename == '' or video_file.filename == '':
        return jsonify({'error': 'Empty filename'}), 400
    
    if not (allowed_file(resume_file.filename) and allowed_file(video_file.filename)):
        return jsonify({'error': 'File type not allowed'}), 400
    
    try:
        # Save the uploaded files
        resume_filename = secure_filename(resume_file.filename)
        resume_path = os.path.join(app.config['UPLOAD_FOLDER'], 
                                  f"{int(time.time())}_resume_{resume_filename}")
        resume_file.save(resume_path)
        
        video_filename = secure_filename(video_file.filename)
        video_path = os.path.join(app.config['UPLOAD_FOLDER'], 
                                f"{int(time.time())}_video_{video_filename}")
        video_file.save(video_path)
        
        # Evaluate the candidate
        result = evaluator.evaluate_candidate(resume_path, cgpa, video_path)
        
        # Clean up
        os.remove(resume_path)
        os.remove(video_path)
        
        return jsonify(result)
    
    except Exception as e:
        logger.error(f"Error evaluating candidate: {e}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/evaluation_state', methods=['GET'])
def get_evaluation_state():
    """API endpoint to get the current evaluation state"""
    try:
        state = evaluator.get_evaluation_state()
        return jsonify(state)
    except Exception as e:
        logger.error(f"Error getting evaluation state: {str(e)}")
        return jsonify({'error': f'Failed to get evaluation state: {str(e)}'}), 500


@app.route('/api/reset_evaluation', methods=['POST'])
def reset_evaluation():
    """API endpoint to reset the evaluation state"""
    try:
        state = evaluator.reset_evaluation()
        return jsonify({
            'status': 'success',
            'message': 'Evaluation reset successfully',
            'state': state
        })
    except Exception as e:
        logger.error(f"Error resetting evaluation: {str(e)}")
        return jsonify({'error': f'Failed to reset evaluation: {str(e)}'}), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=False)
