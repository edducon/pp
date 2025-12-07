from __future__ import annotations

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User

router = Router()


@router.message(Command("notifications"))
async def notification_settings(message: Message, translator, session: AsyncSession) -> None:
    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await message.answer(translator.t("errors.not_registered"))
        return
    start = user.notification_window_start.strftime("%H:%M") if user.notification_window_start else "09:00"
    end = user.notification_window_end.strftime("%H:%M") if user.notification_window_end else "22:00"
    await message.answer(translator.t("notifications.window", start=start, end=end))
