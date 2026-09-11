FROM python:3.12-slim

WORKDIR /app

# Install system utilities and build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpcap-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY edge/ ./edge/
COPY training/ ./training/

ENV PYTHONPATH=/app
ENV VAJRA_DB_PATH=/app/vajraworld.db

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "edge.api.server:app", "--host", "0.0.0.0", "--port", "8000"]
