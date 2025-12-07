from __future__ import annotations

from datetime import datetime

from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()


@router.message(Command("documents"))
async def list_documents(message: Message, translator, session: AsyncSession) -> None:
    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await message.answer(translator.t("errors.not_registered"))
        return
    docs_result = await session.execute(select(UserDocument).where(UserDocument.user_id == user.id))
    docs = docs_result.scalars().all()
    if not docs:
        await message.answer(translator.t("documents.empty"))
        return
    lines = []
    for doc in docs:
        doc_name = doc.document_type.name_ru if translator.language == "ru" else doc.document_type.name_en
        expiry = doc.current_expiry_date.isoformat() if doc.current_expiry_date else "—"
        lines.append(translator.t("documents.item", name=doc_name, expiry=expiry))
    await message.answer("\n".join(lines))
