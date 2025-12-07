from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from aiogram import Bot, Dispatcher
from aiogram.client.default import DefaultBotProperties
from aiogram.enums import ParseMode
from aiogram.fsm.storage.memory import MemoryStorage
from aiogram.fsm.strategy import FSMStrategy

from bot.db import SessionLocal
from bot.handlers import admin, errors, menu, onboarding
from bot.i18n.loader import TranslationLoader
from bot.middlewares.localization import LocalizationMiddleware
from bot.middlewares.rate_limit import RateLimitMiddleware
from bot.services.reminder_service import ReminderService
from config import get_settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def main() -> None:
    settings = get_settings()
    locales_path = Path(__file__).parent / "bot" / "locales"
    translation_loader = TranslationLoader(locales_path)
    translation_loader.load()

    bot = Bot(token=settings.bot_token, default=DefaultBotProperties(parse_mode=ParseMode.HTML))
    storage = MemoryStorage()
    dp = Dispatcher(storage=storage, fsm_strategy=FSMStrategy.CHAT)

    dp.message.middleware(RateLimitMiddleware())
    dp.message.middleware(LocalizationMiddleware(translation_loader))
    dp.callback_query.middleware(LocalizationMiddleware(translation_loader))

    dp.include_router(onboarding.router)
    dp.include_router(menu.router)
    dp.include_router(admin.router)
    dp.include_router(errors.router)

    reminder = ReminderService(bot, SessionLocal, settings.timezone, translation_loader)

    try:
        await reminder.start()
        await dp.start_polling(bot, translation_loader=translation_loader)
    finally:
        await reminder.shutdown()
        await bot.session.close()


if __name__ == "__main__":
    asyncio.run(main())
