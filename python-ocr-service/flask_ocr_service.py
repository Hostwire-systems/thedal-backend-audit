#!/usr/bin/env python3
"""
Flask OCR Service - Web API for Page 4 Photo Extractor
Provides REST endpoints for voter photo extraction from PDF files
Optimized for cloud deployment with memory management
"""

from flask import Flask, request, jsonify, send_file
from flask_cors import CORS
import os
import logging
import tempfile
import shutil
import uuid
import gc
import psutil
from datetime import datetime
from extract_page4 import Page4PhotoExtractor
import json
from werkzeug.utils import secure_filename
from config import (
    MAX_FILE_SIZE, TEMP_FILE_MAX_AGE_HOURS, RESULTS_MAX_AGE_HOURS,
    JPEG_QUALITY, ENABLE_DEBUG_IMAGES, get_config_summary
)
from advanced_photo_extractor import AdvancedPhotoExtractor

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Use absolute paths relative to the project root directory
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
UPLOAD_FOLDER = os.path.join(PROJECT_ROOT, 'temp_uploads')
RESULTS_FOLDER = os.path.join(PROJECT_ROOT, 'extraction_results')

# Ensure directories exist
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(RESULTS_FOLDER, exist_ok=True)

# Initialize extractors (advanced will be lazy-loaded). We now default to the advanced path
# automatically unless a client explicitly disables it with advanced=0.
extractor = Page4PhotoExtractor()
advanced_extractor = None  # lazy init when first needed

def get_memory_usage():
    """Get current memory usage in MB."""
    process = psutil.Process(os.getpid())
    return process.memory_info().rss / 1024 / 1024

def allowed_file(filename):
    """Check if file extension is allowed."""
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'pdf'

def cleanup_temp_files(temp_dir, max_age_hours=TEMP_FILE_MAX_AGE_HOURS):
    """Clean up old temporary files."""
    try:
        current_time = datetime.now()
        files_cleaned = 0
        for root, dirs, files in os.walk(temp_dir):
            for file in files:
                file_path = os.path.join(root, file)
                file_time = datetime.fromtimestamp(os.path.getctime(file_path))
                age_hours = (current_time - file_time).total_seconds() / 3600
                
                if age_hours > max_age_hours:
                    os.remove(file_path)
                    files_cleaned += 1
                    logger.info(f"Cleaned up old file: {file}")
        
        if files_cleaned > 0:
            logger.info(f"🧹 Cleaned up {files_cleaned} old files")
            gc.collect()  # Force garbage collection after cleanup
            
    except Exception as e:
        logger.error(f"Error during cleanup: {e}")

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint with memory usage and configuration info."""
    memory_mb = get_memory_usage()
    return jsonify({
        'status': 'healthy',
        'service': 'OCR Photo Extraction Service',
        'version': '1.1.0',
        'timestamp': datetime.now().isoformat(),
        'memory_usage_mb': round(memory_mb, 2),
        'features': {
            'chunked_processing': True,
            'memory_cleanup': True,
            'optimized_for_cloud': True
        },
        'configuration': get_config_summary()
    })

@app.route('/extract-photos', methods=['POST'])
def extract_photos():
    """
    Extract voter photos from uploaded PDF.
    
    Expected request:
    - file: PDF file
    - part_no: Part number (optional)
    - election_id: Election ID (optional)
    - job_id: Job tracking ID (optional)
    - start_page: Starting page number for extraction (1-based, optional, defaults to 3)
    - end_page: Ending page number for extraction (1-based, optional, defaults to second-to-last page)
    
    Returns:
    - job_id: Unique job identifier
    - photos: List of extracted photo data with serial numbers
    - metadata: Extraction metadata
    """
    try:
        # Validate request
        if 'file' not in request.files:
            return jsonify({'error': 'No file provided'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'error': 'No file selected'}), 400
        
        if not allowed_file(file.filename):
            return jsonify({'error': 'Invalid file type. Only PDF files are allowed.'}), 400
        
        # Get additional parameters
        part_no = request.form.get('part_no', '')
        election_id = request.form.get('election_id', '')
        job_id = request.form.get('job_id', str(uuid.uuid4()))
        
        # Get page range parameters
        start_page = None
        end_page = None
        
        if 'start_page' in request.form:
            try:
                start_page = int(request.form['start_page'])
                if start_page < 1:
                    return jsonify({'error': 'start_page must be at least 1'}), 400
            except ValueError:
                return jsonify({'error': 'start_page must be a valid integer'}), 400
                
        if 'end_page' in request.form:
            try:
                end_page = int(request.form['end_page'])
                if end_page < 1:
                    return jsonify({'error': 'end_page must be at least 1'}), 400
            except ValueError:
                return jsonify({'error': 'end_page must be a valid integer'}), 400
        
        # Validate page range
        if start_page is not None and end_page is not None and start_page >= end_page:
            return jsonify({'error': 'start_page must be less than end_page'}), 400
        
        # Log memory usage before processing
        initial_memory = get_memory_usage()
        logger.info(f"Starting photo extraction - Job ID: {job_id}, Part: {part_no}, Election: {election_id}")
        logger.info(f"📊 Initial memory usage: {initial_memory:.2f} MB")
        if start_page is not None or end_page is not None:
            logger.info(f"📄 Custom page range: start={start_page}, end={end_page}")
        
        # Create job-specific directory
        job_dir = os.path.join(RESULTS_FOLDER, job_id)
        os.makedirs(job_dir, exist_ok=True)
        
        # Save uploaded file
        filename = secure_filename(file.filename)
        temp_pdf_path = os.path.join(UPLOAD_FOLDER, f"{job_id}_{filename}")
        file.save(temp_pdf_path)
        
        # Extract photos using production page extractor
        logger.info(f"Processing PDF: {filename}")
        
        # Determine pages to process
        pages_to_process = []
        if start_page is not None and end_page is not None:
            # Process all pages in the range
            pages_to_process = list(range(start_page, end_page + 1))
            logger.info(f"Processing pages {start_page} to {end_page}: {len(pages_to_process)} pages")
        elif start_page is not None and end_page is None:
            # Single page specified
            pages_to_process = [start_page]
        elif start_page is None and end_page is not None:
            # Only end page specified
            pages_to_process = [end_page]
        else:
            # No page specified, default to page 4
            pages_to_process = [4]

        # Determine whether to use the advanced extractor for this request.
        # Priority order: explicit form param (advanced=0 disables) > default True.
        advanced_param = request.form.get('advanced')
        if advanced_param is not None:
            use_advanced = (advanced_param == '1')
        else:
            use_advanced = True  # default to best extractor automatically

        # Optional override for photo detector mode (e.g., 'onnx')
        requested_photo_detector = request.form.get('photo_detector')
        if requested_photo_detector:
            logger.info(f"Photo detector override requested: {requested_photo_detector}")
        logger.info(f"Advanced extractor enabled for this request: {use_advanced}")
        
        # Process all pages and collect results
        all_photos = []
        total_extracted = 0
        page_range_start = pages_to_process[0]  # First page in range for serial number calculation
        
        for page_to_process in pages_to_process:
            logger.info(f"Processing page {page_to_process}... (advanced={use_advanced})")
            local_advanced_extractor = advanced_extractor
            if use_advanced and local_advanced_extractor is None:
                # Lazy load advanced extractor only when first needed
                try:
                    logger.info("Initializing advanced extractor on-demand")
                    from advanced_photo_extractor import AdvancedPhotoExtractor as _A
                    local_advanced_extractor = _A(enable_dnn=True, photo_detector_mode=requested_photo_detector)
                    globals()['advanced_extractor'] = local_advanced_extractor
                except Exception as e:
                    logger.error(f"Failed to init advanced extractor, falling back: {e}")
                    use_advanced = False  # disable for remainder if initialization failed
            if use_advanced and local_advanced_extractor is not None:
                # If user supplied a detector mode different from the original, and object lacks that mode, re-init once.
                if requested_photo_detector and getattr(local_advanced_extractor, 'photo_detector_mode','') != requested_photo_detector.lower():
                    try:
                        logger.info(f"Re-initializing advanced extractor for detector mode {requested_photo_detector}")
                        from advanced_photo_extractor import AdvancedPhotoExtractor as _A
                        local_advanced_extractor = _A(enable_dnn=True, photo_detector_mode=requested_photo_detector)
                        globals()['advanced_extractor'] = local_advanced_extractor
                    except Exception as e:
                        logger.warning(f"Could not re-init advanced extractor with new detector mode: {e}")
                extraction_results = local_advanced_extractor.extract_photos(temp_pdf_path, page_to_process)
                # Fallback to basic extractor if advanced produced zero photos for this page
                if extraction_results.get('extracted_count', 0) == 0:
                    logger.warning(f"Advanced extractor produced 0 photos on page {page_to_process}; falling back to basic extractor for this page.")
                    extraction_results = extractor.extract_page4_photos(temp_pdf_path, page_to_process)
            else:
                extraction_results = extractor.extract_page4_photos(temp_pdf_path, page_to_process)
            
            # Process photos from this page
            for photo_data in extraction_results['photos']:
                # Calculate the correct serial number for this card based on the page range
                cards_per_page = 30
                page_offset = page_to_process - page_range_start  # 0 for first page in range
                calculated_serial_no = page_offset * cards_per_page + photo_data['card_number']
                
                logger.info(f"📊 Photo mapping: Card {photo_data['card_number']} on page {page_to_process} → Serial {calculated_serial_no}")
                logger.info(f"    Formula: {page_offset} * {cards_per_page} + {photo_data['card_number']} = {calculated_serial_no}")
                
                # Create the file path directly in the job directory (no intermediate folders)
                source_path = os.path.join(extraction_results['output_dir'], f"card{photo_data['card_number']:02d}_serial{photo_data['serial_number']}_{photo_data['method']}.jpg")
                target_filename = f"voter_{calculated_serial_no:03d}_{photo_data['size']}.jpg"
                target_path = os.path.join(job_dir, target_filename)
                
                # Copy the file directly to the job directory
                if os.path.exists(source_path):
                    shutil.copy2(source_path, target_path)
                
                formatted_photo = {
                    'card_id': photo_data['card_number'],
                    'serial_number': calculated_serial_no,
                    'method': photo_data['method'],
                    'dimensions': photo_data['size'],
                    'confidence': photo_data.get('confidence', 1.0),
                    'page_number': page_to_process,
                    'path': target_path,
                    'filename': target_filename
                }
                all_photos.append(formatted_photo)
            
            total_extracted += extraction_results['extracted_count']
            
            # Clean up the temporary page extraction folder
            try:
                if os.path.exists(extraction_results['output_dir']):
                    shutil.rmtree(extraction_results['output_dir'])
            except Exception as e:
                logger.warning(f"Could not clean up temp folder: {e}")
        
        # Create consolidated results
        formatted_results = {
            'saved_photos': all_photos,
            'total_photos': total_extracted,
            'success_rate': (total_extracted / (len(pages_to_process) * 30)) * 100 if pages_to_process else 0
        }
        
        if not formatted_results['saved_photos']:
            return jsonify({
                'jobId': job_id,  # Use camelCase to match Java expectations
                'success': False,
                'error': 'No photos could be extracted from the PDF'
            }), 500
        
        # Use the consolidated results
        extraction_results = formatted_results
        
        # Process extracted photos and assign serial numbers
        processed_photos = []
        logger.info(f"Processing {len(extraction_results['saved_photos'])} photos from {len(pages_to_process)} page(s)")
        
        for i, photo_data in enumerate(extraction_results['saved_photos']):
            # Get serial number (already calculated in the previous step)
            serial_no_for_data = photo_data['serial_number']
            
            # Format for filename (already done in previous step)
            serial_no_formatted = f"{serial_no_for_data:03d}"
            
            # File should already be correctly named
            current_path = photo_data['path']
            expected_filename = f"voter_{serial_no_formatted}_{photo_data['dimensions']}.jpg"
            
            # Check if file needs renaming (should not be needed now)
            if not os.path.basename(current_path).startswith('voter_'):
                new_path = os.path.join(job_dir, expected_filename)
                shutil.move(current_path, new_path)
                final_path = new_path
                final_filename = expected_filename
            else:
                final_path = current_path
                final_filename = os.path.basename(current_path)
            
            # Card position should be based on the position within the page (1-30), not absolute serial number
            card_position_on_page = photo_data['card_id']  # This is 1-30 for positions on the current page
            
            processed_photos.append({
                'serial_no': serial_no_for_data,  # Use the calculated serial number
                'filename': final_filename,
                'filePath': final_path,  # Use camelCase to match Java field names
                'dimensions': photo_data['dimensions'],
                'extractionMethod': photo_data['method'],  # Use camelCase
                'confidence': photo_data.get('confidence', 1.0),
                'pageNumber': photo_data.get('page_number', pages_to_process[0]),  # Use actual processed page
                'cardPosition': {  # Use camelCase to match Java field names
                    'row': (card_position_on_page - 1) // 3 + 1,  # 3 cards per row
                    'col': (card_position_on_page - 1) % 3 + 1   # Position within row
                }
            })
        
        # Create extraction metadata
        metadata = {
            'jobId': job_id,  # Use camelCase for consistency
            'partNo': part_no,
            'electionId': election_id,
            'pdfFilename': filename,
            'extractionTimestamp': datetime.now().isoformat(),
            'totalPhotosExtracted': len(processed_photos),
            'pagesProcessed': len(pages_to_process),
            'extractionAccuracy': f"{(len([p for p in processed_photos if p['confidence'] > 0.7])/len(processed_photos)*100) if processed_photos else 0:.1f}%",
            'processingSummary': {
                'totalCards': len(processed_photos),
                'photosFound': len(processed_photos),
                'highConfidence': len([p for p in processed_photos if p['confidence'] > 0.7]),
                'mediumConfidence': len([p for p in processed_photos if 0.5 <= p['confidence'] <= 0.7]),
                'lowConfidence': len([p for p in processed_photos if p['confidence'] < 0.5])
            }
        }
        
        # Save metadata
        metadata_path = os.path.join(job_dir, 'extraction_metadata.json')
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        # Cleanup temp PDF
        try:
            os.remove(temp_pdf_path)
        except:
            pass
        
        # Final memory cleanup and monitoring
        gc.collect()
        final_memory = get_memory_usage()
        memory_diff = final_memory - initial_memory
        
        logger.info(f"Photo extraction completed - Job ID: {job_id}, Pages: {pages_to_process}, Photos: {len(processed_photos)}")
        logger.info(f"📊 Final memory usage: {final_memory:.2f} MB (diff: {memory_diff:+.2f} MB)")
        
        return jsonify({
            'jobId': job_id,  # Use camelCase to match Java expectations
            'success': True,
            'photos': processed_photos,
            'metadata': metadata,
            'message': f'Successfully extracted {len(processed_photos)} photos'
        })
        
    except Exception as e:
        logger.error(f"Error during photo extraction: {str(e)}")
        return jsonify({
            'jobId': job_id if 'job_id' in locals() else None,  # Use camelCase
            'success': False,
            'error': f'Internal server error: {str(e)}'
        }), 500

@app.route('/job-status/<job_id>', methods=['GET'])
def get_job_status(job_id):
    """Get status of a specific job."""
    try:
        job_dir = os.path.join(RESULTS_FOLDER, job_id)
        metadata_path = os.path.join(job_dir, 'extraction_metadata.json')
        
        if not os.path.exists(metadata_path):
            return jsonify({'error': 'Job not found'}), 404
        
        with open(metadata_path, 'r') as f:
            metadata = json.load(f)
        
        # Count available photo files
        available_photos = []
        if os.path.exists(job_dir):
            for file in os.listdir(job_dir):
                if file.startswith('voter_') and file.endswith('.jpg'):
                    available_photos.append(file)
        
        return jsonify({
            'job_id': job_id,
            'status': 'completed',
            'metadata': metadata,
            'available_photos': len(available_photos)
        })
        
    except Exception as e:
        logger.error(f"Error getting job status: {str(e)}")
        return jsonify({'error': f'Error retrieving job status: {str(e)}'}), 500

@app.route('/download-photo/<job_id>/<int:serial_no>', methods=['GET'])
def download_photo(job_id, serial_no):
    """Download a specific photo by serial number."""
    try:
        job_dir = os.path.join(RESULTS_FOLDER, job_id)
        
        # Find photo file for serial number
        photo_file = None
        for file in os.listdir(job_dir):
            if file.startswith(f'voter_{serial_no:03d}_') and file.endswith('.jpg'):
                photo_file = file
                break
        
        if not photo_file:
            return jsonify({'error': f'Photo for serial number {serial_no} not found'}), 404
        
        photo_path = os.path.join(job_dir, photo_file)
        return send_file(photo_path, as_attachment=True, download_name=photo_file)
        
    except Exception as e:
        logger.error(f"Error downloading photo: {str(e)}")
        return jsonify({'error': f'Error downloading photo: {str(e)}'}), 500

@app.route('/download-all/<job_id>', methods=['GET'])
def download_all_photos(job_id):
    """Download all photos as a zip file."""
    try:
        job_dir = os.path.join(RESULTS_FOLDER, job_id)
        
        if not os.path.exists(job_dir):
            return jsonify({'error': 'Job not found'}), 404
        
        # Create zip file
        import zipfile
        zip_path = os.path.join(job_dir, f'voter_photos_{job_id}.zip')
        
        with zipfile.ZipFile(zip_path, 'w') as zipf:
            for file in os.listdir(job_dir):
                if file.endswith('.jpg'):
                    file_path = os.path.join(job_dir, file)
                    zipf.write(file_path, file)
        
        return send_file(zip_path, as_attachment=True, download_name=f'voter_photos_{job_id}.zip')
        
    except Exception as e:
        logger.error(f"Error creating zip file: {str(e)}")
        return jsonify({'error': f'Error creating zip file: {str(e)}'}), 500

@app.route('/cleanup', methods=['POST'])
def cleanup_old_files():
    """Cleanup old temporary and result files with memory monitoring."""
    try:
        initial_memory = get_memory_usage()
        
        cleanup_temp_files(UPLOAD_FOLDER, TEMP_FILE_MAX_AGE_HOURS)
        cleanup_temp_files(RESULTS_FOLDER, RESULTS_MAX_AGE_HOURS)
        
        final_memory = get_memory_usage()
        
        return jsonify({
            'success': True,
            'message': 'Cleanup completed successfully',
            'memory_freed_mb': round(initial_memory - final_memory, 2),
            'final_memory_mb': round(final_memory, 2)
        })
        
    except Exception as e:
        logger.error(f"Error during cleanup: {str(e)}")
        return jsonify({'error': f'Cleanup error: {str(e)}'}), 500

@app.errorhandler(413)
def too_large(e):
    """Handle file too large error."""
    max_mb = MAX_FILE_SIZE // (1024 * 1024)
    return jsonify({'error': f'File too large. Maximum size is {max_mb}MB.'}), 413

if __name__ == '__main__':
    logger.info("Starting OCR Photo Extraction Service...")
    logger.info(f"Upload folder: {UPLOAD_FOLDER}")
    logger.info(f"Results folder: {RESULTS_FOLDER}")
    logger.info(f"Project root: {PROJECT_ROOT}")
    logger.info(f"Configuration: {get_config_summary()}")
    
    # Run cleanup on startup
    cleanup_temp_files(UPLOAD_FOLDER)
    
    # Start Flask app
    app.run(
        host='0.0.0.0',
        port=5000,
        debug=False,
        threaded=True
    )
