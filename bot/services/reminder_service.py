from __future__ import annotations

import logging
from datetime import datetime, time
from typing import Callable

from aiogram import Bot
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from pytz import timezone
from sqlalchemy.ext.asyncio import AsyncSession

from bot.i18n.loader import TranslationLoader
from bot.models import UserDocument
from bot.services.document_service import DocumentService

logger = logging.getLogger(__name__)


class ReminderService:
    def __init__(
        self,
        bot: Bot,
        session_factory: Callable[[], AsyncSession],
        tz_name: str,
        translation_loader: TranslationLoader,
    ) -> None:
        self.bot = bot
        self.session_factory = session_factory
        self.scheduler = AsyncIOScheduler(timezone=timezone(tz_name))
        self.tz = timezone(tz_name)
        self.translation_loader = translation_loader

    async def start(self) -> None:
        self.scheduler.add_job(self._dispatch_reminders, IntervalTrigger(minutes=30))
        self.scheduler.start()

    async def shutdown(self) -> None:
        self.scheduler.shutdown(wait=False)

    async def _dispatch_reminders(self) -> None:
        async with self.session_factory() as session:
            doc_service = DocumentService(session)
            now = datetime.now(self.tz)
            for doc in await doc_service.next_reminders(now, self.tz):
                await self._notify_for_document(session, doc, now)
            await session.commit()

    async def _notify_for_document(self, session: AsyncSession, doc: UserDocument, now: datetime) -> None:
        user = doc.user
        translator = self.translation_loader.get_translator(user.language)
        window_start = user.notification_time_start
        window_end = user.notification_time_end
        if not self._within_window(now.time(), window_start, window_end):
            return
        try:
            days_left = (doc.expiry_date - now.date()).days if doc.expiry_date else None
            message = self._format_message(translator.t, doc.document_type.code, days_left, doc.submitted_for_extension)
            await self.bot.send_message(chat_id=user.telegram_id, text=message)
            doc.last_notification_at = now
        except Exception as exc:  # noqa: BLE001
            logger.exception("Failed to send reminder: %s", exc)

    def _format_message(self, t, doc_code: str, days_left: int | None, submitted: bool) -> str:
        if submitted:
            return t("reminders.submitted")
        return t(f"reminders.{doc_code.lower()}", days=days_left)

    def _within_window(self, now: time, start: time, end: time) -> bool:
        if start <= end:
            return start <= now <= end
        return now >= start or now <= end
