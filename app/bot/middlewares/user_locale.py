from __future__ import annotations

from typing import Any, Awaitable, Callable, Dict

from aiogram import BaseMiddleware
from aiogram.types import TelegramObject
from sqlalchemy import select

from app.bot.i18n.loader import TranslationLoader
from app.models.user import User


class UserLocaleMiddleware(BaseMiddleware):
    def __init__(self, loader: TranslationLoader) -> None:
        super().__init__()
        self.loader = loader

    async def __call__(
        self,
        handler: Callable[[TelegramObject, Dict[str, Any]], Awaitable[Any]],
        event: TelegramObject,
        data: Dict[str, Any],
    ) -> Any:
        session = data.get("session")
        language = "ru"
        if session:
            user_id = getattr(getattr(event, "from_user", None), "id", None)
            if user_id:
                result = await session.execute(select(User).where(User.telegram_id == user_id))
                db_user = result.scalar_one_or_none()
                if db_user:
                    language = db_user.language
        translator = self.loader.get_translator(language)
        data["translator"] = translator
        data["_"] = translator.t
        return await handler(event, data)
