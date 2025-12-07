from __future__ import annotations

import time
from typing import Any, Awaitable, Callable, Dict

from aiogram import BaseMiddleware
from aiogram.types import TelegramObject


class ThrottlingMiddleware(BaseMiddleware):
    def __init__(self, limit: float = 1.0) -> None:
        super().__init__()
        self.limit = limit
        self.last_time: Dict[int, float] = {}

    async def __call__(
        self,
        handler: Callable[[TelegramObject, Dict[str, Any]], Awaitable[Any]],
        event: TelegramObject,
        data: Dict[str, Any],
    ) -> Any:
        user_id = getattr(getattr(event, "from_user", None), "id", 0)
        now = time.monotonic()
        if user_id:
            previous = self.last_time.get(user_id, 0)
            if now - previous < self.limit:
                translator = data.get("translator")
                bot = data.get("bot")
                if translator and bot:
                    await bot.send_message(chat_id=user_id, text=translator.t("errors.too_many_requests"))
                return
            self.last_time[user_id] = now
        return await handler(event, data)
