# Migration Reminder Bot

Асинхронный Telegram-бот на aiogram 3.x, который напоминает иностранным гражданам о сроках миграционной карты, временной регистрации, медицинских сертификатов и визы.

## Структура проекта
```
app/
  main.py
  config.py
  logging_config.py
  bot/
    handlers/
      start.py
      profile.py
      documents.py
      notifications.py
      admin.py
    middlewares/
      user_locale.py
      db_session.py
      throttling.py
    keyboards/
      inline.py
      reply.py
    states/
      user_states.py
    services/
      notifications_service.py
      broadcast_service.py
      document_service.py
    i18n/
      loader.py
  models/
    base.py
    user.py
    documents.py
    __init__.py
  locales/
    ru.json
    en.json
migrations/
  env.py
  versions/
    0001_initial.py
requirements.txt
alembic.ini
.env.example
Dockerfile
```

## Установка зависимостей
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Подготовка окружения
Скопируйте `.env.example` в `.env` и заполните значения (токен бота, строка подключения к БД, ID супер-админа и т.д.).

## PostgreSQL через docker-compose
```bash
docker-compose up -d db
```

## Миграции Alembic
```bash
alembic upgrade head
```

## Запуск бота локально
```bash
python -m app.main
```

## Развёртывание
1. Настройте переменные окружения на сервере (можно использовать `.env`).
2. Установите зависимости в изолированном окружении.
3. Примените миграции Alembic к продакшен-базе данных.
4. Запустите процесс бота (systemd/pm2/supervisor) командой `python -m app.main`.
5. Убедитесь, что каталог для логов доступен и ротация файлов включена.
