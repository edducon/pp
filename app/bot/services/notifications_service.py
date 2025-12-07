from __future__ import annotations

from datetime import date, datetime, time, timedelta
from typing import Iterable

import pytz
from aiogram import Bot
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.i18n.loader import TranslationLoader
from app.models.documents import DocumentType, UserDocument
from app.models.user import User


DEFAULT_WINDOW = (time(hour=9), time(hour=22))


class NotificationsService:
    def __init__(self, bot: Bot, session_factory, timezone: str, i18n: TranslationLoader) -> None:
        self.bot = bot
        self.session_factory = session_factory
        self.scheduler = AsyncIOScheduler(timezone=timezone)
        self.tz = pytz.timezone(timezone)
        self.i18n = i18n

    async def start(self) -> None:
        self.scheduler.add_job(self._tick, IntervalTrigger(minutes=60))
        self.scheduler.start()

    async def shutdown(self) -> None:
        if self.scheduler.running:
            self.scheduler.shutdown(wait=False)

    async def _tick(self) -> None:
        async with self.session_factory() as session:  # type: AsyncSession
            await self._send_due_notifications(session)

    async def _send_due_notifications(self, session: AsyncSession) -> None:
        now = datetime.now(self.tz)
        today = now.date()
        stmt = select(UserDocument).where(UserDocument.notifications_enabled.is_(True))
        result = await session.execute(stmt)
        docs: Iterable[UserDocument] = result.scalars()
        for doc in docs:
            user: User = doc.user
            if not user:
                continue
            if not self._within_window(now.time(), user):
                continue
            if not doc.current_expiry_date:
                continue
            days_left = (doc.current_expiry_date - today).days
            start_offset = 45 if doc.document_type and doc.document_type.code == "VISA" else 30
            if days_left < -1:
                continue
            translator = self.i18n.get_translator(user.language)
            doc_name = doc.document_type.name_ru if translator.language == "ru" else doc.document_type.name_en
            if days_left <= 1 and not doc.final_reminder_sent:
                await self.bot.send_message(user.telegram_id, translator.t("notifications.expired", doc_name=doc_name))
                doc.final_reminder_sent = True
            elif days_left <= start_offset:
                await self.bot.send_message(
                    user.telegram_id,
                    translator.t("notifications.reminder", doc_name=doc_name, days_left=days_left),
                )
                doc.last_notification_at = now
        await session.commit()

    def _within_window(self, now: time, user: User) -> bool:
        start = user.notification_window_start or DEFAULT_WINDOW[0]
        end = user.notification_window_end or DEFAULT_WINDOW[1]
        return start <= now <= end
