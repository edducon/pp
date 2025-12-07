from __future__ import annotations

import asyncio
import time
from typing import Any, Awaitable, Callable, Dict

from aiogram import BaseMiddleware
from aiogram.types import Message

from config import get_settings


class RateLimitMiddleware(BaseMiddleware):
    def __init__(self) -> None:
        super().__init__()
        self.settings = get_settings()
        self._buckets: dict[int, list[float]] = {}
        self._lock = asyncio.Lock()

    async def __call__(
        self,
        handler: Callable[[Message, Dict[str, Any]], Awaitable[Any]],
        event: Message,
        data: Dict[str, Any],
    ) -> Any:
        user_id = event.from_user.id
        async with self._lock:
            timestamps = self._buckets.setdefault(user_id, [])
            now = time.time()
            window_start = now - 60
            self._buckets[user_id] = [ts for ts in timestamps if ts >= window_start]
            if len(self._buckets[user_id]) >= self.settings.rate_limit_per_minute:
                return
            self._buckets[user_id].append(now)
        return await handler(event, data)
