from __future__ import annotations
from datetime import datetime, timedelta

from aiogram import Bot
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, AsyncSession

from app.bot.i18n.loader import Translator
from app.bot.services.document_service import within_window, parse_time_window
from app.models.documents import DocumentType, UserDocument
from app.models.user import User


REMINDER_STEPS = [45, 30, 21, 14, 7, 3, 1]


class NotificationService:
    def __init__(
        self,
        bot: Bot,
        sessionmaker: async_sessionmaker[AsyncSession],
        translator: Translator,
        timezone: str,
        default_window_start: str,
        default_window_end: str,
    ):
        self.bot = bot
        self.sessionmaker = sessionmaker
        self.translator = translator
        self.timezone = timezone
        self.default_start, self.default_end = parse_time_window(f"{default_window_start}-{default_window_end}")

    async def run(self) -> None:
        async with self.sessionmaker() as session:
            await self._process_documents(session)

    async def _process_documents(self, session: AsyncSession) -> None:
        result = await session.execute(
            select(UserDocument, User, DocumentType)
            .join(User, User.id == UserDocument.user_id)
            .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        )
        now = datetime.now(tz=datetime.utcnow().astimezone().tzinfo)
        for doc, user, doc_type in result.all():
            await self._handle_document(now, session, doc, user, doc_type)
        await session.commit()

    async def _handle_document(
        self, now: datetime, session: AsyncSession, doc: UserDocument, user: User, doc_type: DocumentType
    ) -> None:
        if not doc.notifications_enabled or doc.submitted_for_extension:
            return
        if doc.current_expiry_date is None:
            return

        window_start = user.notification_window_start or self.default_start
        window_end = user.notification_window_end or self.default_end
        if not within_window(now, window_start, window_end, self.timezone):
            return

        days_left = (doc.current_expiry_date - now.date()).days
        language = user.language or self.translator.fallback_language
        doc_name = doc_type.name_ru if language.startswith("ru") else doc_type.name_en

        if days_left < 0 and not doc.final_reminder_sent:
            text = self.translator.gettext(language, "notifications.expired", doc_name=doc_name)
            await self.bot.send_message(user.telegram_id, text)
            doc.final_reminder_sent = True
            return

        threshold = 45 if doc_type.code == "VISA" else 30
        if days_left > threshold:
            return

        if days_left in REMINDER_STEPS:
            if doc.last_notification_at and (now - doc.last_notification_at) < timedelta(days=1):
                return
            text = self.translator.gettext(
                language,
                "notifications.reminder",
                doc_name=doc_name,
                date=self.translator.format_date(doc.current_expiry_date, language),
                days=days_left,
            )
            await self.bot.send_message(user.telegram_id, text)
            doc.last_notification_at = now
            return

    @staticmethod
    def add_job(scheduler, service: "NotificationService", minutes: int) -> None:
        scheduler.add_job(service.run, "interval", minutes=minutes, id="notifications")
