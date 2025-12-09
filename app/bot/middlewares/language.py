from __future__ import annotations

from aiogram import BaseMiddleware
from aiogram.types import TelegramObject
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import User
from app.services.localization import Translator


class LanguageMiddleware(BaseMiddleware):
    def __init__(self, translator: Translator):
        self.translator = translator

    async def __call__(self, handler, event: TelegramObject, data: dict):
        session: AsyncSession | None = data.get("session")
        locale = "ru"
        if session and event.from_user:
            stmt = select(User.language).where(User.telegram_id == event.from_user.id)
            res = await session.execute(stmt)
            locale = res.scalar_one_or_none() or locale
        data["locale"] = locale
        data["t"] = self.translator.t
        return await handler(event, data)
