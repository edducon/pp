from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from aiogram import Bot, Dispatcher
from aiogram.client.default import DefaultBotProperties
from aiogram.enums import ParseMode
from aiogram.fsm.storage.memory import MemoryStorage
from aiogram.fsm.strategy import FSMStrategy

from app.bot.handlers import admin, documents, notifications, profile, start
from app.bot.i18n.loader import TranslationLoader
from app.bot.middlewares.db_session import DBSessionMiddleware
from app.bot.middlewares.throttling import ThrottlingMiddleware
from app.bot.middlewares.user_locale import UserLocaleMiddleware
from app.bot.services.notifications_service import NotificationsService
from app.config import get_settings
from app.db import SessionLocal
from app.logging_config import setup_logging


async def main() -> None:
    settings = get_settings()
    setup_logging()
    logger = logging.getLogger(__name__)

    locales_path = Path(__file__).parent / "locales"
    translation_loader = TranslationLoader(locales_path)
    translation_loader.load()

    bot = Bot(token=settings.bot_token, default=DefaultBotProperties(parse_mode=ParseMode.HTML))
    storage = MemoryStorage()
    dp = Dispatcher(storage=storage, fsm_strategy=FSMStrategy.CHAT)

    dp.update.middleware(ThrottlingMiddleware())
    dp.update.middleware(DBSessionMiddleware())
    dp.update.middleware(UserLocaleMiddleware(translation_loader))

    dp.include_router(start.router)
    dp.include_router(profile.router)
    dp.include_router(documents.router)
    dp.include_router(notifications.router)
    dp.include_router(admin.router)

    notifier = NotificationsService(bot, SessionLocal, settings.timezone, translation_loader)

    logger.info("Bot starting")
    try:
        await notifier.start()
        await dp.start_polling(
            bot,
            allowed_updates=dp.resolve_used_update_types(),
            translation_loader=translation_loader,
        )
    finally:
        await notifier.shutdown()
        await bot.session.close()


if __name__ == "__main__":
    asyncio.run(main())
