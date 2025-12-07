# Migration Reminder Bot

Асинхронный Telegram-бот на aiogram 3.x, помогающий иностранным гражданам в РФ не забывать о сроках миграционных документов.

## Требования
- Python 3.10+
- PostgreSQL

## Установка
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Настройка окружения
Скопируйте `.env.example` в `.env` и заполните значения:
```
BOT_TOKEN=...
DATABASE_URL=postgresql+asyncpg://user:password@localhost:5432/migration_bot
SUPERADMIN_ID=...
TIMEZONE=Europe/Moscow
```

## База данных и миграции
Инициализируйте базу и примените миграции Alembic:
```bash
alembic upgrade head
```

## Запуск бота
```bash
python -m app.main
```

## Запуск PostgreSQL через docker-compose
```bash
docker-compose up -d db
```

## Развёртывание
- Установите зависимости в изолированном окружении.
- Примените миграции к продакшен-базе.
- Настройте systemd/pm2/supervisor для автозапуска `python -m app.main`.
- Убедитесь, что переменные окружения заданы и лог-файлы пишутся в доступный каталог.
