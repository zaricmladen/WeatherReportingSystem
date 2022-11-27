FROM python:3.9-alpine
WORKDIR /app

# Install dependencies:
COPY requirements.txt .
RUN pip3 install -r requirements.txt

# Run the application:
COPY main.py .
CMD ["python3", "main.py"]

