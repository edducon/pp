from __future__ import annotations

import asyncio
from typing import Iterable, Optional

from aiogram import Bot
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User


class BroadcastService:
    def __init__(self, bot: Bot, session: AsyncSession) -> None:
        self.bot = bot
        self.session = session

    async def broadcast(self, text: str, language: Optional[str] = None) -> int:
        stmt = select(User)
        if language:
            stmt = stmt.where(User.language == language)
        result = await self.session.execute(stmt)
        users: Iterable[User] = result.scalars()
        count = 0
        for user in users:
            try:
                await self.bot.send_message(user.telegram_id, text)
                count += 1
                await asyncio.sleep(0.1)
            except Exception:
                continue
        return count
