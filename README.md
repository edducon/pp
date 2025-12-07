# Telegram Document Reminder Bot

Асинхронный Telegram-бот для иностранных граждан: хранит данные документов, напоминает о сроках действия и поддерживает двуязычный интерфейс.

## Стек

- Python 3.10+
- aiogram 3.x (long polling)
- PostgreSQL + SQLAlchemy Async + asyncpg
- Alembic
- APScheduler
- Запланированная зона времени: Europe/Moscow

## Подготовка окружения

1. Установите зависимости:

   ```bash
   pip install -r requirements.txt
   ```

2. Создайте файл `.env` по примеру:

   ```bash
   cp .env.example .env
   ```

   Заполните `BOT_TOKEN`, `DATABASE_URL`, `SUPERADMIN_ID`.

3. Инициализируйте базу данных:

   ```bash
   alembic upgrade head
   ```

   Миграции используют подключения из переменной `DATABASE_URL`.

## Запуск бота локально

```bash
python main.py
```

Бот стартует в режиме long polling. Все уведомления отправляются через APScheduler с часовым поясом Europe/Moscow.

## Структура проекта

- `main.py` — точка входа, настройка диспетчера, middlewares и планировщика.
- `config.py` — загрузка секретов из `.env`.
- `bot/models/` — SQLAlchemy модели пользователей и документов.
- `bot/handlers/` — обработчики команд, онбординга, меню и админ-команд.
- `bot/middlewares/` — локализация и rate-limit.
- `bot/services/` — бизнес-логика (гражданства, документы, напоминания).
- `bot/i18n/` — загрузка переводов, файлы `locales/ru.json` и `locales/en.json`.
- `migrations/` — конфигурация Alembic и первичная миграция.

## Миграции Alembic

Создать новую миграцию:

```bash
alembic revision -m "message"
```

Применить миграции:

```bash
alembic upgrade head
```

Откатить:

```bash
alembic downgrade -1
```

## Полезные команды бота

- `/start` — онбординг с выбором языка, гражданства, телефона и окна уведомлений.
- `/documents` — просмотр документов.
- `/status` — статус профиля.
- `/language` — смена языка.
- `/delete_me` — полное удаление данных.
- `/add_admin` — только для `SUPERADMIN_ID` (назначает администратора по reply).
- `/broadcast` — массовая рассылка администратора.
- `/stats` — сводная статистика.
