from __future__ import annotations

from typing import Any, Callable, Dict, Awaitable

from aiogram import BaseMiddleware
from aiogram.types import CallbackQuery, Message

from bot.i18n.loader import TranslationLoader


class LocalizationMiddleware(BaseMiddleware):
    def __init__(self, loader: TranslationLoader) -> None:
        super().__init__()
        self.loader = loader

    async def __call__(
        self,
        handler: Callable[[Message, Dict[str, Any]], Awaitable[Any]],
        event: Message | CallbackQuery,
        data: Dict[str, Any],
    ) -> Any:
        language = self.loader.get_user_language(event)
        translator = self.loader.get_translator(language)
        data["translator"] = translator
        data["t"] = translator.t
        data["translation_loader"] = self.loader
        return await handler(event, data)
