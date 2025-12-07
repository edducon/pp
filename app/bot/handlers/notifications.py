from aiogram import Router
from aiogram.filters import Command
from aiogram.types import Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()


@router.message(Command("status"))
async def status(message: Message, session: AsyncSession, t, translator, language: str):
    result = await session.execute(
        select(UserDocument, DocumentType)
        .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        .join(User, User.id == UserDocument.user_id)
        .where(User.telegram_id == message.from_user.id)
    )
    rows = result.all()
    if not rows:
        await message.answer(t("status.empty"))
        return
    lines = [t("status.header")]
    for doc, doc_type in rows:
        lines.append(
            t(
                "status.item",
                doc_name=doc_type.name_ru if language.startswith("ru") else doc_type.name_en,
                expiry=translator.format_date(doc.current_expiry_date, language),
                notif=t("buttons.on") if doc.notifications_enabled else t("buttons.off"),
            )
        )
    await message.answer("\n".join(lines))
