from __future__ import annotations

from aiogram import F, Router
from aiogram.filters import Command
from aiogram.types import CallbackQuery, Message

from bot.db import get_session
from bot.handlers.keyboards import main_menu_keyboard
from bot.services.document_service import DocumentService
from bot.services.user_service import UserService

router = Router()


@router.callback_query(F.data.startswith("menu:"))
async def menu_handler(callback: CallbackQuery, t) -> None:
    action = callback.data.split(":")[1]
    if action == "language":
        await callback.message.answer(t("language.switch_hint"))
    elif action == "documents":
        await _show_documents(callback.from_user.id, callback.message, t)
    elif action == "status":
        await _show_status(callback.from_user.id, callback.message, t)
    elif action == "delete":
        await _delete_me(callback.from_user.id, callback.message, t)


@router.message(Command("documents"))
async def documents_command(message: Message, t) -> None:
    await _show_documents(message.from_user.id, message, t)


@router.message(Command("status"))
async def status_command(message: Message, t) -> None:
    await _show_status(message.from_user.id, message, t)


@router.message(Command("language"))
async def language_command(message: Message, t) -> None:
    await message.answer(t("language.switch_hint"), reply_markup=main_menu_keyboard(t))


@router.message(Command("delete_me"))
async def delete_command(message: Message, t) -> None:
    await _delete_me(message.from_user.id, message, t)


async def _show_documents(user_id: int, message: Message, t) -> None:
    async with get_session() as session:
        user_service = UserService(session)
        doc_service = DocumentService(session)
        user = await user_service.get_by_telegram(user_id)
        docs = await doc_service.get_user_documents(user)
    lines = [t("documents.header")]
    for doc in docs:
        name = doc.document_type.name_ru if t("language.code") == "ru" else doc.document_type.name_en
        status = t("documents.status_missing") if not doc.expiry_date else doc.expiry_date.isoformat()
        lines.append(f"• {name}: {status}")
    await message.answer("\n".join(lines), reply_markup=main_menu_keyboard(t))


async def _show_status(user_id: int, message: Message, t) -> None:
    async with get_session() as session:
        user_service = UserService(session)
        user = await user_service.get_by_telegram(user_id)
    await message.answer(t("status.summary", citizenship=user.citizenship_code), reply_markup=main_menu_keyboard(t))


async def _delete_me(user_id: int, message: Message, t) -> None:
    async with get_session() as session:
        user_service = UserService(session)
        user = await user_service.get_by_telegram(user_id)
        if user:
            await user_service.delete_user(user)
            await session.commit()
            await message.answer(t("profile.deleted"))
        else:
            await message.answer(t("profile.not_found"))
    await message.answer(t("onboarding.choose_language"), reply_markup=main_menu_keyboard(t))
