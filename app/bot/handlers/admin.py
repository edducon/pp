from __future__ import annotations

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.models.user import User

router = Router()
settings = get_settings()


@router.message(Command("add_admin"))
async def add_admin(message: Message, translator, session: AsyncSession) -> None:
    if message.from_user.id != settings.superadmin_id:
        return
    parts = message.text.split()
    if len(parts) < 2:
        await message.answer(translator.t("admin.enter_user_id"))
        return
    target_id = int(parts[1])
    result = await session.execute(select(User).where(User.telegram_id == target_id))
    user = result.scalar_one_or_none()
    if not user:
        await message.answer(translator.t("admin.user_not_found"))
        return
    user.is_admin = True
    await session.commit()
    await message.answer(translator.t("admin.added"))
