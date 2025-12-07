from aiogram import BaseMiddleware
from aiogram.types import CallbackQuery, Message, TelegramObject
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.i18n.loader import Translator
from app.models.user import User


class UserLocaleMiddleware(BaseMiddleware):
    def __init__(self, translator: Translator):
        super().__init__()
        self.translator = translator

    async def __call__(self, handler, event: TelegramObject, data: dict):
        language = self.translator.fallback_language
        telegram_id = None
        if isinstance(event, Message) and event.from_user:
            telegram_id = event.from_user.id
        elif isinstance(event, CallbackQuery) and event.from_user:
            telegram_id = event.from_user.id

        session: AsyncSession | None = data.get("session")
        user: User | None = None
        if telegram_id and session is not None:
            result = await session.execute(select(User).where(User.telegram_id == telegram_id))
            user = result.scalar_one_or_none()
            if user:
                language = user.language or language
                data["user"] = user

        data["language"] = language
        data["t"] = lambda key, **kwargs: self.translator.gettext(language, key, **kwargs)
        data["translator"] = self.translator
        return await handler(event, data)
