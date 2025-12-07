from __future__ import annotations

import logging

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message

from bot.db import get_session
from bot.services.user_service import UserService
from config import get_settings

router = Router()
logger = logging.getLogger(__name__)


@router.message(Command("add_admin"))
async def add_admin(message: Message, t) -> None:
    settings = get_settings()
    if message.from_user.id != settings.superadmin_id:
        await message.answer(t("admin.not_allowed"))
        return
    reply_to = message.reply_to_message
    if not reply_to:
        await message.answer(t("admin.reply_required"))
        return
    async with get_session() as session:
        user_service = UserService(session)
        user = await user_service.get_by_telegram(reply_to.from_user.id)
        if not user:
            await message.answer(t("admin.user_not_found"))
            return
        user.is_admin = True
        await session.commit()
    await message.answer(t("admin.promoted"))


@router.message(Command("broadcast"))
async def broadcast(message: Message, t, bot) -> None:
    async with get_session() as session:
        user_service = UserService(session)
        sender = await user_service.get_by_telegram(message.from_user.id)
        if not sender or not sender.is_admin:
            await message.answer(t("admin.not_allowed"))
            return
        from sqlalchemy import select
        from bot.models import User

        result = await session.execute(select(User.telegram_id))
        ids = [row[0] for row in result.all()]
    for chat_id in ids:
        try:
            await bot.copy_message(chat_id=chat_id, from_chat_id=message.chat.id, message_id=message.message_id)
        except Exception as exc:  # noqa: BLE001
            logger.exception("Failed to broadcast: %s", exc)
    await message.answer(t("admin.broadcast_done"))


@router.message(Command("stats"))
async def stats(message: Message, t) -> None:
    async with get_session() as session:
        from sqlalchemy import func
        from bot.models import User, UserDocument

        user_service = UserService(session)
        sender = await user_service.get_by_telegram(message.from_user.id)
        if not sender or not sender.is_admin:
            await message.answer(t("admin.not_allowed"))
            return
        total_users = (await session.execute(func.count(User.id))).scalar_one()
        total_docs = (await session.execute(func.count(UserDocument.id))).scalar_one()
    await message.answer(t("admin.stats", users=total_users, docs=total_docs))
