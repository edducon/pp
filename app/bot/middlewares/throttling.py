import time
from collections import defaultdict, deque
from typing import Deque

from aiogram import BaseMiddleware
from aiogram.types import Message, CallbackQuery, TelegramObject


class ThrottlingMiddleware(BaseMiddleware):
    def __init__(self, limit: int = 5, interval: int = 3):
        super().__init__()
        self.limit = limit
        self.interval = interval
        self.storage: dict[int, Deque[float]] = defaultdict(deque)

    async def __call__(self, handler, event: TelegramObject, data: dict):
        user_id = None
        if isinstance(event, Message) and event.from_user:
            user_id = event.from_user.id
        elif isinstance(event, CallbackQuery) and event.from_user:
            user_id = event.from_user.id

        if user_id is None:
            return await handler(event, data)

        now = time.monotonic()
        queue = self.storage[user_id]
        while queue and now - queue[0] > self.interval:
            queue.popleft()
        if len(queue) >= self.limit:
            t = data.get("t") or (lambda key, **kwargs: "Too many requests")
            if isinstance(event, CallbackQuery):
                await event.answer(t("errors.too_many_requests"), show_alert=False)
            elif isinstance(event, Message):
                await event.answer(t("errors.too_many_requests"))
            return None

        queue.append(now)
        return await handler(event, data)
