from __future__ import annotations

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User

router = Router()


@router.message(Command("status"))
async def status(message: Message, translator, session: AsyncSession) -> None:
    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await message.answer(translator.t("errors.not_registered"))
        return
    await message.answer(
        translator.t(
            "profile.summary",
            citizenship=user.citizenship_name or translator.t("profile.not_set"),
            phone=user.phone or translator.t("profile.not_set"),
        )
    )
