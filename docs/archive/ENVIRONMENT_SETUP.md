# Environment Variables Configuration Guide

This guide explains how to configure the Thedal application using environment variables for better security and deployment flexibility.

## 🔒 Security Benefits

By moving credentials to environment variables, we achieve:
- **No hardcoded secrets** in source code
- **Secure deployment** across different environments
- **Easy credential rotation** without code changes
- **Better compliance** with security best practices

## 📝 Setup Instructions

### 1. Local Development

1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and fill in your actual values:
   ```bash
   nano .env
   # or use your preferred editor
   ```

3. Run the application (environment variables will be loaded automatically):
   ```bash
   mvn spring-boot:run
   ```

### 2. Docker Deployment

1. Create your environment file:
   ```bash
   cp .env.example .env
   # Edit .env with your production values
   ```

2. Run with Docker Compose:
   ```bash
   docker-compose up -d
   ```

### 3. Production Deployment

For production deployment, you can:

#### Option A: Use .env.production file
```bash
cp .env.production .env
# Edit .env with your actual production credentials
docker-compose up -d
```

#### Option B: Set environment variables directly
```bash
export DB_PASSWORD="your_secure_password"
export AWS_S3_SECRET_KEY="your_aws_secret"
# ... other variables
docker-compose up -d
```

#### Option C: Use CI/CD secrets
In your CI/CD pipeline (GitHub Actions, GitLab CI, etc.), set the environment variables as secrets.

## 🔧 Environment Variables Reference

### Database Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/thedal` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | _(required)_ |
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017` |
| `MONGODB_DATABASE` | MongoDB database name | `thedal_db` |

### AWS Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `AWS_S3_ACCESS_KEY` | AWS S3 Access Key | _(required)_ |
| `AWS_S3_SECRET_KEY` | AWS S3 Secret Key | _(required)_ |
| `AWS_S3_REGION` | AWS S3 Region | `ap-south-1` |
| `AWS_ACCESS_KEY` | AWS General Access Key | _(required)_ |
| `AWS_SECRET_KEY` | AWS General Secret Key | _(required)_ |
| `AWS_SQS_QUEUE_URL` | AWS SQS Queue URL | _(required)_ |

### Payment Gateway
| Variable | Description | Default |
|----------|-------------|---------|
| `CASHFREE_CLIENT_ID` | Cashfree Client ID | _(required)_ |
| `CASHFREE_CLIENT_SECRET` | Cashfree Client Secret | _(required)_ |

### Email Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `MAIL_USERNAME` | SMTP Email Username | _(required)_ |
| `MAIL_PASSWORD` | SMTP Email Password | _(required)_ |
| `SENDGRID_KEY` | SendGrid API Key | _(required)_ |
| `SENDGRID_EMAIL` | SendGrid From Email | _(required)_ |

### Redis Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis Host | `localhost` |
| `REDIS_PORT` | Redis Port | `6379` |
| `REDIS_PASSWORD` | Redis Password | _(required for prod)_ |
| `REDIS_SSL` | Enable Redis SSL | `false` |

### SMS Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `MSG91_AUTHKEY` | MSG91 Auth Key | _(required)_ |
| `MSG91_TEMPLATE_ID` | MSG91 Template ID | _(required)_ |

### OAuth Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| `GOOGLE_OAUTH_CLIENT_ID` | Google OAuth Client ID | _(required)_ |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth Client Secret | _(required)_ |

## 🚀 Deployment Examples

### Local Development
```bash
# .env file
DB_PASSWORD=mylocal_password
AWS_S3_ACCESS_KEY=dev_access_key
AWS_S3_SECRET_KEY=dev_secret_key
SENDGRID_KEY=dev_sendgrid_key
```

### Staging Environment
```bash
# .env file
DB_URL=jdbc:postgresql://staging-db:5432/thedal_staging
DB_PASSWORD=staging_secure_password
THEDAL_UI_URL=https://staging.thedal.co.in/
THEDAL_SERVER_URL=https://api-staging.thedal.co.in/
```

### Production Environment
```bash
# .env file
DB_URL=jdbc:postgresql://prod-db:5432/thedal_prod?sslmode=require
DB_PASSWORD=super_secure_production_password
REDIS_SSL=true
THEDAL_UI_URL=https://thedal.co.in/
THEDAL_SERVER_URL=https://api.thedal.co.in/
```

## 🔐 Security Best Practices

1. **Never commit .env files** to version control
2. **Use different credentials** for each environment
3. **Rotate credentials regularly**
4. **Use strong, unique passwords**
5. **Enable SSL/TLS** for production databases
6. **Use secrets management** in cloud deployments (AWS Secrets Manager, Azure Key Vault, etc.)

## 🛡️ .gitignore Configuration

Make sure your `.gitignore` includes:
```
# Environment variables
.env
.env.local
.env.production
.env.staging

# Logs
*.log
logs/

# IDE
.vscode/
.idea/
```

## 🐳 Docker Best Practices

### Multi-stage builds
The Dockerfile already uses multi-stage builds for smaller, more secure images.

### Run as non-root user
Consider adding this to your Dockerfile:
```dockerfile
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring
```

### Health checks
The docker-compose.yml includes health checks for better monitoring.

## 📊 Monitoring

You can monitor environment variable usage by checking:
- Application startup logs
- Health check endpoints
- Configuration actuator endpoints (if enabled)

## 🆘 Troubleshooting

### Common Issues

1. **Application fails to start**
   - Check if all required environment variables are set
   - Verify database connectivity
   - Check logs for missing credentials

2. **Database connection issues**
   - Verify `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
   - Check network connectivity
   - Verify SSL settings for production

3. **AWS services not working**
   - Check AWS credentials and permissions
   - Verify bucket names and regions
   - Check IAM policies

### Debugging Commands

```bash
# Check if environment variables are loaded
docker-compose exec thedal-app env | grep -E "DB_|AWS_|REDIS_"

# Check application logs
docker-compose logs thedal-app

# Test database connection
docker-compose exec thedal-app curl http://localhost:8080/actuator/health
```

## 📞 Support

If you encounter issues with environment variable configuration:
1. Check the logs for specific error messages
2. Verify all required variables are set
3. Ensure credentials are valid and have proper permissions
4. Consult the application documentation for specific requirements
