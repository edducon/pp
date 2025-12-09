from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from aiogram import Bot, Dispatcher
from aiogram.enums import ParseMode
from aiogram.fsm.storage.memory import MemoryStorage
from aiogram.client.default import DefaultBotProperties  # <-- новая строка
from apscheduler.schedulers.asyncio import AsyncIOScheduler

from app.bot.handlers import admin, user
from app.bot.middlewares.db import DbSessionMiddleware
from app.bot.middlewares.language import LanguageMiddleware
from app.config import load_settings
from app.database import Database
from app.logging import setup_logging
from app.services.countries import CountryService
from app.services.localization import Translator
from app.services.notifier import Notifier


async def main() -> None:
    setup_logging()
    settings = load_settings()

    # Локализация
    translator = Translator(Path(__file__).parent / "locales")
    translator.load()

    # Страны
    country_service = CountryService(
        Path(__file__).parent / "data" / "countries.json"
    )
    country_service.load_dataset()

    # Бот и диспетчер
    bot = Bot(
        token=settings.token,
        # parse_mode больше нельзя передавать напрямую —
        # используем default=DefaultBotProperties(...)
        default=DefaultBotProperties(parse_mode=ParseMode.HTML),
    )
    storage = MemoryStorage()
    dp = Dispatcher(storage=storage)

    # База данных
    database = Database(settings)

    # Миграции у нас бегут через `alembic upgrade head` в docker-compose,
    # здесь только используем сессию.
    async with database.session() as session:
        await country_service.sync(session)

    # Передаём сервис стран в хендлеры пользователя
    user.setup_country_service(country_service)

    # Мидлвари
    dp.update.outer_middleware(DbSessionMiddleware(database.session_factory))
    dp.update.outer_middleware(LanguageMiddleware(translator))
    dp.workflow_data.update({"settings": settings})

    # Роутеры
    dp.include_router(user.router)
    dp.include_router(admin.router)

    # Планировщик уведомлений
    scheduler = AsyncIOScheduler(timezone=settings.timezone)
    notifier = Notifier(scheduler, database.session_factory, bot, translator)
    notifier.start()

    logging.info("Bot starting")
    await dp.start_polling(bot, settings=settings, translator=translator)


if __name__ == "__main__":
    asyncio.run(main())
