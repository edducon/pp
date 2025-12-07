import asyncio
from pathlib import Path

from aiogram import Bot, Dispatcher
from aiogram.client.default import DefaultBotProperties
from aiogram.fsm.storage.memory import MemoryStorage
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.bot.handlers import admin, documents, notifications, profile, start
from app.bot.i18n.loader import TranslationLoader
from app.bot.middlewares.db_session import DbSessionMiddleware
from app.bot.middlewares.throttling import ThrottlingMiddleware
from app.bot.middlewares.user_locale import UserLocaleMiddleware
from app.bot.services.document_service import ensure_document_types
from app.bot.services.notifications_service import NotificationService
from app.config import load_settings
from app.logging_config import setup_logging


async def main() -> None:
    settings = load_settings()
    setup_logging(settings.log_file)

    locales_dir = Path(__file__).parent / "locales"
    translator = TranslationLoader(locales_dir).load()

    engine = create_async_engine(settings.database_url, echo=False, future=True)
    sessionmaker = async_sessionmaker(engine, expire_on_commit=False)

    async with sessionmaker() as session:
        await ensure_document_types(session)

    bot = Bot(token=settings.bot_token, default=DefaultBotProperties(parse_mode="HTML"))
    bot["sessionmaker"] = sessionmaker
    bot["settings"] = settings

    storage = MemoryStorage()
    dp = Dispatcher(storage=storage)

    dp.update.middleware(DbSessionMiddleware(sessionmaker))
    dp.update.middleware(UserLocaleMiddleware(translator))
    dp.message.middleware(ThrottlingMiddleware())
    dp.callback_query.middleware(ThrottlingMiddleware())

    dp.include_router(start.router)
    dp.include_router(profile.router)
    dp.include_router(documents.router)
    dp.include_router(notifications.router)
    dp.include_router(admin.router)

    scheduler = AsyncIOScheduler(timezone=settings.timezone)
    notification_service = NotificationService(
        bot,
        sessionmaker=sessionmaker,
        translator=translator,
        timezone=settings.timezone,
        default_window_start=settings.default_notification_start,
        default_window_end=settings.default_notification_end,
    )
    NotificationService.add_job(scheduler, notification_service, minutes=settings.scheduler_interval_minutes)
    scheduler.start()

    await bot.delete_webhook(drop_pending_updates=True)
    await dp.start_polling(bot, allowed_updates=dp.resolve_used_update_types())


if __name__ == "__main__":
    asyncio.run(main())
