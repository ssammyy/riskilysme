# Deployment Guideline

This document outlines the step-by-step workflow for deploying the **Riskily SME** application to your production/staging server using Docker, Docker Compose, and your Docker registry.

---

## High-Level Architecture
The deployment uses Docker Compose to run:
1. **Database**: PostgreSQL (packaged in `docker-compose.yml`).
2. **Backend**: Kotlin/Spring Boot backend container running the JAR.
3. **Frontend**: Vite/React single-page application served via Nginx (reverse-proxying `/api` requests to the backend).

---

## Pre-requisites on the Server
Make sure the following are installed on your destination server:
* **Docker** (version 20.10+)
* **Docker Compose** (V2, i.e., `docker compose` command)

---

## Deployment Workflow

### Step 1: Build & Push Images (Locally or CI/CD)

To deployment-proof the images for host/platform compatibility (e.g. deploying from Apple Silicon to a standard `linux/amd64` server), build using the target platform tag.

1. **Log in to Docker Hub** (if not already logged in):
   ```bash
   docker login
   ```

2. **Build and Tag the Backend Image**:
   ```bash
   docker build --platform linux/amd64 -t samik254/riskily-backend:latest ./backend
   ```

3. **Build and Tag the Frontend Image**:
   ```bash
   docker build --platform linux/amd64 -t samik254/riskily-frontend:latest ./frontend
   ```

4. **Push the Images to Docker Hub**:
   ```bash
   docker push samik254/riskily-backend:latest
   docker push samik254/riskily-frontend:latest
   ```

---

### Step 2: Configure the Server Environment

1. **Log in to your Server** via SSH:
   ```bash
   ssh user@your-server-ip
   ```

2. **Prepare the Deployment Directory**:
   Ensure you have a folder containing your `docker-compose.yml` and `.env` files.
   * If they are not already on the server, copy them from your project root.

3. **Configure Environment Variables & Secrets**:
   Create or edit the `.env` file on the server. Make sure it contains values for the newly required API keys/secrets:
   ```env
   # Database Configurations
   POSTGRES_DB=riskily_sme
   POSTGRES_USER=riskily
   POSTGRES_PASSWORD=your_secure_db_password
   POSTGRES_PORT=5432

   # Backend Configurations
   SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/riskily_sme
   SPRING_DATASOURCE_USERNAME=riskily
   SPRING_DATASOURCE_PASSWORD=your_secure_db_password
   SERVER_PORT=8080

   # Authentication Secrets
   # Generate a unique, strong 256-bit key for production!
   JWT_SECRET=your_super_secret_jwt_sign_key_minimum_32_bytes_long
   JWT_ACCESS_TTL_MINUTES=15
   JWT_REFRESH_TTL_DAYS=14

   # Third-Party API Keys (Required for complete functionality)
   RESEND_API_KEY=re_yourResendApiKeyHere
   CLAUDE_API_KEY=sk-ant-yourClaudeApiKeyHere
   ```

   > [!WARNING]
   > Ensure that the `.env` file is **never** committed to version control. It contains sensitive credentials.

---

### Step 3: Pull & Deploy on the Server

Run these commands inside the deployment directory on your server:

1. **Pull the latest images** from Docker Hub:
   ```bash
   docker compose pull
   ```

2. **Restart the containers** in detached mode:
   ```bash
   docker compose down
   docker compose up -d
   ```

3. **Verify the Status**:
   Ensure all containers are running and healthy:
   ```bash
   docker compose ps
   ```

4. **Check logs to verify startup**:
   ```bash
   docker compose logs -f backend
   ```
