from __future__ import annotations
from datetime import datetime

from aiogram import F, Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.inline import document_actions_keyboard
from app.bot.states.user_states import DocumentStates
from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()


def parse_date(value: str, language: str) -> datetime.date:
    fmt = "%d.%m.%Y" if language.startswith("ru") else "%Y-%m-%d"
    return datetime.strptime(value.strip(), fmt).date()


def doc_display_name(doc_type: DocumentType, language: str) -> str:
    return doc_type.name_ru if language.startswith("ru") else doc_type.name_en


@router.message(Command("documents"))
async def list_documents(message: Message, session: AsyncSession, t, language: str, translator):
    result = await session.execute(
        select(UserDocument, DocumentType)
        .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        .join(User, User.id == UserDocument.user_id)
        .where(User.telegram_id == message.from_user.id)
    )
    docs = result.all()
    if not docs:
        await message.answer(t("documents.none"))
        return
    for doc, doc_type in docs:
        expiry_text = translator.format_date(doc.current_expiry_date, language)
        text = t(
            "documents.item",
            doc_name=doc_display_name(doc_type, language),
            expiry=expiry_text,
            status=t("documents.extension", status=t("buttons.yes") if doc.submitted_for_extension else t("buttons.no")),
        )
        await message.answer(text, reply_markup=document_actions_keyboard(doc.id))


@router.callback_query(F.data.startswith("doc:expiry:"))
async def ask_new_expiry(callback: CallbackQuery, state: FSMContext, t):
    doc_id = int(callback.data.split(":")[-1])
    await state.update_data(doc_id=doc_id)
    await state.set_state(DocumentStates.set_expiry_date)
    await callback.message.answer(t("documents.ask_expiry"))


@router.message(DocumentStates.set_expiry_date)
async def save_new_expiry(message: Message, state: FSMContext, session: AsyncSession, t, language: str):
    data = await state.get_data()
    doc_id = data.get("doc_id")
    if not doc_id:
        await message.answer(t("errors.general"))
        return
    try:
        expiry = parse_date(message.text or "", language)
    except ValueError:
        await message.answer(t("errors.invalid_date"))
        return
    result = await session.execute(
        select(UserDocument).join(User).where(UserDocument.id == doc_id, User.telegram_id == message.from_user.id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        await message.answer(t("errors.general"))
        return
    doc.current_expiry_date = expiry
    doc.submitted_for_extension = False
    doc.final_reminder_sent = False
    await session.commit()
    await message.answer(t("documents.expiry_saved"))
    await state.clear()


@router.callback_query(F.data.startswith("doc:toggle:"))
async def toggle_notifications(callback: CallbackQuery, session: AsyncSession, t):
    doc_id = int(callback.data.split(":")[-1])
    result = await session.execute(
        select(UserDocument).join(User).where(UserDocument.id == doc_id, User.telegram_id == callback.from_user.id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        await callback.answer(t("errors.general"))
        return
    doc.notifications_enabled = not doc.notifications_enabled
    await session.commit()
    await callback.message.answer(
        t("documents.notifications_switched", state=t("buttons.on") if doc.notifications_enabled else t("buttons.off"))
    )


@router.callback_query(F.data.startswith("doc:extend:"))
async def submit_extension(callback: CallbackQuery, session: AsyncSession, t):
    doc_id = int(callback.data.split(":")[-1])
    result = await session.execute(
        select(UserDocument).join(User).where(UserDocument.id == doc_id, User.telegram_id == callback.from_user.id)
    )
    doc = result.scalar_one_or_none()
    if not doc:
        await callback.answer(t("errors.general"))
        return
    doc.submitted_for_extension = True
    await session.commit()
    await callback.message.answer(t("documents.submitted"))
