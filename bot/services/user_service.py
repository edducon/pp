from __future__ import annotations

from datetime import time
from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from bot.models import User


class UserService:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_telegram(self, telegram_id: int) -> Optional[User]:
        result = await self.session.execute(select(User).where(User.telegram_id == telegram_id))
        return result.scalars().first()

    async def create_user(
        self,
        telegram_id: int,
        language: str,
        citizenship_code: str,
        phone: str | None,
        window_start: time,
        window_end: time,
        is_admin: bool = False,
    ) -> User:
        user = User(
            telegram_id=telegram_id,
            language=language,
            citizenship_code=citizenship_code,
            phone=phone,
            notification_time_start=window_start,
            notification_time_end=window_end,
            is_admin=is_admin,
        )
        self.session.add(user)
        await self.session.flush()
        return user

    async def update_language(self, user: User, language: str) -> None:
        user.language = language
        await self.session.flush()

    async def delete_user(self, user: User) -> None:
        await self.session.delete(user)
        await self.session.flush()
