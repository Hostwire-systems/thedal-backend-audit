# Use Python 3.11 slim image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install minimal system dependencies for OpenCV headless, PDF processing, and OCR
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libgomp1 \
    tesseract-ocr \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first to leverage Docker cache
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directories for temporary files and uploads
RUN mkdir -p /app/temp_uploads /app/extraction_results

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV FLASK_APP=flask_ocr_service.py
ENV FLASK_ENV=production

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:5000/health')" || exit 1

# Run the application with Gunicorn for production
CMD ["python", "start_production.py"]
