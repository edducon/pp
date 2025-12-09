from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from aiogram import Bot, Dispatcher
from aiogram.enums import ParseMode
from aiogram.fsm.storage.memory import MemoryStorage
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
    translator = Translator(Path(__file__).parent / "locales")
    translator.load()
    country_service = CountryService(Path(__file__).parent / "data" / "countries.json")
    country_service.load_dataset()

    bot = Bot(token=settings.token, parse_mode=ParseMode.HTML)
    storage = MemoryStorage()
    dp = Dispatcher(storage=storage)

    database = Database(settings)
    await database.run_migrations()
    async with database.session() as session:
        await country_service.sync(session)

    user.setup_country_service(country_service)

    dp.update.outer_middleware(DbSessionMiddleware(database.session_factory))
    dp.update.outer_middleware(LanguageMiddleware(translator))
    dp.workflow_data.update({"settings": settings})

    dp.include_router(user.router)
    dp.include_router(admin.router)

    scheduler = AsyncIOScheduler(timezone=settings.timezone)
    notifier = Notifier(scheduler, database.session_factory, bot, translator)
    notifier.start()

    logging.info("Bot starting")
    await dp.start_polling(bot, settings=settings, translator=translator)


if __name__ == "__main__":
    asyncio.run(main())
