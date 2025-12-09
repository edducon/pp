FROM python:3.12-slim

WORKDIR /app
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1

# при необходимости — компилятор и заголовки (часто нужны для некоторых пакетов)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
 && rm -rf /var/lib/apt/lists/*

# зависимости проекта
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# сам код
COPY . .

# по умолчанию запускаем бота;
# docker-compose поверх этого всё равно задаёт свою command
CMD ["python", "-m", "main.py"]
