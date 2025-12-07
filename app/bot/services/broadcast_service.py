from __future__ import annotations
import asyncio
import logging
from typing import Iterable

from aiogram import Bot
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.models.documents import DocumentType, UserDocument
from app.models.user import User

logger = logging.getLogger(__name__)


class BroadcastService:
    def __init__(self, bot: Bot, sessionmaker: async_sessionmaker[AsyncSession]):
        self.bot = bot
        self.sessionmaker = sessionmaker
        self.delay = 0.05

    async def send_broadcast(
        self,
        text: str,
        language: str | None = None,
        citizenship: str | None = None,
        document_code: str | None = None,
        photo: bytes | None = None,
        session: AsyncSession | None = None,
    ) -> tuple[int, int]:
        if session is None:
            async with self.sessionmaker() as session:
                return await self._send(session, text, language, citizenship, document_code, photo)
        return await self._send(session, text, language, citizenship, document_code, photo)

    async def _send(
        self,
        session: AsyncSession,
        text: str,
        language: str | None,
        citizenship: str | None,
        document_code: str | None,
        photo: bytes | None,
    ) -> tuple[int, int]:
        sent = 0
        failed = 0
        users = await self._get_users(session, language, citizenship, document_code)
        for user in users:
            try:
                if photo:
                    await self.bot.send_photo(user.telegram_id, photo=photo, caption=text)
                else:
                    await self.bot.send_message(user.telegram_id, text)
                sent += 1
            except Exception as exc:  # pragma: no cover
                logger.error("Broadcast failed for %s: %s", user.telegram_id, exc)
                failed += 1
            await asyncio.sleep(self.delay)
        logger.info("Broadcast finished: sent=%s failed=%s", sent, failed)
        return sent, failed

    async def _get_users(
        self, session: AsyncSession, language: str | None, citizenship: str | None, document_code: str | None
    ) -> Iterable[User]:
        query = select(User)
        if language:
            query = query.where(User.language == language)
        if citizenship:
            query = query.where(User.citizenship_code == citizenship)
        if document_code:
            query = query.join(UserDocument).join(DocumentType).where(DocumentType.code == document_code)
        result = await session.execute(query)
        return result.scalars().all()
