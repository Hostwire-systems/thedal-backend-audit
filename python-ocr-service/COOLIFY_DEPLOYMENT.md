# Coolify Configuration for Python OCR Service
# This file provides guidance for deploying the Python OCR service with Coolify

## Prerequisites
- Git repository access
- Coolify instance running
- Docker support enabled

## Deployment Steps

### 1. Create New Application in Coolify
1. Go to your Coolify dashboard
2. Click "New Application"
3. Choose "Git Repository" as source
4. Connect your repository: https://github.com/Hostwire-systems/thedal-backend

### 2. Application Configuration
- **Name**: `thedal-python-ocr-service`
- **Port**: `5000`
- **Build Pack**: Docker
- **Base Directory**: `python-ocr-service`
- **Dockerfile Location**: `Dockerfile` (relative to base directory)
- **Docker Build Stage Target**: (leave empty)

### 3. Environment Variables
Set these in Coolify:
```
FLASK_ENV=production
PYTHONUNBUFFERED=1
```

### 4. Resource Configuration
- **Memory**: 1GB minimum (2GB recommended for image processing)
- **CPU**: 1 core minimum
- **Storage**: 5GB for temporary files and extraction results

### 5. Health Check
- **Path**: `/health`
- **Port**: `5000`
- **Initial Delay**: 30 seconds
- **Timeout**: 10 seconds

### 6. Domain Configuration
- Set up custom domain or use Coolify provided domain
- Ensure the Spring Boot app points to this service URL

### 7. Persistent Storage (Optional)
If you want to persist extraction results:
- Mount volume: `/app/extraction_results`

## Spring Boot Configuration Update
Update your application.properties:
```
thedal.ocr.service.url=https://your-ocr-service-domain.com
```

## Monitoring
- Check logs in Coolify dashboard
- Monitor health endpoint: https://your-domain.com/health
- Watch resource usage during processing

## Scaling Considerations
- For high load, consider horizontal scaling
- Monitor memory usage during PDF processing
- Consider implementing queue system for batch processing
