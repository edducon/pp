from __future__ import annotations
from datetime import datetime
from typing import Any

from aiogram import F, Router
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.inline import citizenship_options, confirm_keyboard, language_keyboard, notification_windows_keyboard
from app.bot.keyboards.reply import contact_keyboard, main_menu_keyboard, remove_keyboard
from app.bot.services.document_service import ensure_user_documents, parse_time_window
from app.bot.states.user_states import RegistrationStates
from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()


CITIZENSHIP_DATA = [
    {"code": "KAZ", "names": ["казахстан", "kazakhstan", "kaz", "каз"]},
    {"code": "KGZ", "names": ["kyrgyzstan", "киргизия", "кыргызстан", "kg", "kgz"]},
    {"code": "UZB", "names": ["uzbekistan", "узбекистан", "uzb", "uz"]},
    {"code": "ARM", "names": ["armenia", "армения", "arm", "armenia"]},
    {"code": "TJK", "names": ["tajikistan", "таджикистан", "tjk", "tj"]},
]

NOTIFICATION_WINDOWS = [
    ("09:00-18:00", "09:00–18:00"),
    ("09:00-22:00", "09:00–22:00"),
    ("12:00-20:00", "12:00–20:00"),
]


async def get_or_create_user(session: AsyncSession, telegram_id: int, username: str | None) -> User:
    result = await session.execute(select(User).where(User.telegram_id == telegram_id))
    user = result.scalar_one_or_none()
    if user is None:
        user = User(telegram_id=telegram_id, username=username)
        session.add(user)
        await session.commit()
    await ensure_user_documents(session, user)
    return user


def search_citizenship(query: str) -> list[tuple[str, str]]:
    normalized = query.lower().strip()
    matches: list[tuple[str, str]] = []
    for item in CITIZENSHIP_DATA:
        for name in item["names"]:
            if normalized.startswith(name) or name.startswith(normalized) or normalized in name:
                matches.append((item["code"], item["names"][0].capitalize()))
                break
    return matches


def parse_date_by_language(value: str, language: str) -> datetime.date:
    fmt = "%d.%m.%Y" if language.startswith("ru") else "%Y-%m-%d"
    return datetime.strptime(value.strip(), fmt).date()


@router.message(CommandStart())
async def cmd_start(message: Message, state: FSMContext, session: AsyncSession, t, translator, language: str):
    user = await get_or_create_user(session, message.from_user.id, message.from_user.username)
    await state.set_state(RegistrationStates.choose_language)
    await message.answer(t("start.choose_language"), reply_markup=language_keyboard())


@router.callback_query(F.data.startswith("lang:"))
async def choose_language(callback: CallbackQuery, state: FSMContext, session: AsyncSession, translator, t):
    lang_code = callback.data.split(":", 1)[1]
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        user.language = lang_code
        await session.commit()
    await state.set_state(RegistrationStates.enter_citizenship)
    await callback.message.edit_reply_markup()
    await callback.message.answer(
        translator.gettext(lang_code, "start.welcome"),
        reply_markup=remove_keyboard(),
    )
    await callback.message.answer(translator.gettext(lang_code, "start.ask_citizenship"))


@router.message(RegistrationStates.enter_citizenship)
async def ask_citizenship(message: Message, state: FSMContext, session: AsyncSession, t, language: str):
    matches = search_citizenship(message.text or "")
    if not matches:
        await message.answer(t("errors.citizenship_not_found"))
        return
    if len(matches) == 1:
        code, name = matches[0]
        await state.update_data(citizenship_code=code, citizenship_name=name)
        await state.set_state(RegistrationStates.confirm_citizenship)
        await message.answer(t("start.confirm_citizenship", country=name), reply_markup=confirm_keyboard())
        return
    await state.update_data(candidates=matches)
    await message.answer(t("start.choose_from_list"), reply_markup=citizenship_options(matches))


@router.callback_query(F.data.startswith("cit:"))
async def citizenship_choice(callback: CallbackQuery, state: FSMContext, t):
    code = callback.data.split(":", 1)[1]
    if code == "none":
        await state.set_state(RegistrationStates.enter_citizenship)
        await callback.message.answer(t("errors.citizenship_not_found"))
        return
    data = await state.get_data()
    candidates: list[tuple[str, str]] = data.get("candidates", [])
    for cand_code, name in candidates:
        if cand_code == code:
            await state.update_data(citizenship_code=code, citizenship_name=name)
            await state.set_state(RegistrationStates.confirm_citizenship)
            await callback.message.answer(t("start.confirm_citizenship", country=name), reply_markup=confirm_keyboard())
            return
    await callback.answer()


@router.callback_query(F.data.startswith("confirm:"))
async def citizenship_confirm(callback: CallbackQuery, state: FSMContext, session: AsyncSession, t, translator):
    answer = callback.data.split(":", 1)[1]
    data = await state.get_data()
    if answer == "no":
        await state.set_state(RegistrationStates.enter_citizenship)
        await callback.message.answer(t("start.ask_citizenship"))
        return
    code = data.get("citizenship_code")
    name = data.get("citizenship_name")
    if not code or not name:
        await callback.message.answer(t("errors.citizenship_not_found"))
        await state.set_state(RegistrationStates.enter_citizenship)
        return
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        user.citizenship_code = code
        user.citizenship_name = name
        await session.commit()
    await state.set_state(RegistrationStates.enter_phone)
    await callback.message.answer(t("start.ask_phone"), reply_markup=contact_keyboard(t))


@router.message(RegistrationStates.enter_phone)
async def receive_phone(message: Message, state: FSMContext, session: AsyncSession, t):
    phone = None
    if message.contact:
        phone = message.contact.phone_number
    elif message.text:
        if message.text.lower() == t("buttons.skip").lower():
            phone = None
        else:
            phone = message.text.strip()
    if phone and not phone.startswith("+7"):
        await message.answer(t("errors.phone_not_ru"))
        return

    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        user.phone = phone
        await session.commit()
    await state.set_state(RegistrationStates.choose_notification_window)
    options = [(code, t("start.notification_window", start=label.split("–")[0], end=label.split("–")[1])) for code, label in NOTIFICATION_WINDOWS]
    await message.answer(
        t("start.ask_notifications_window"), reply_markup=notification_windows_keyboard(options)
    )


@router.callback_query(F.data.startswith("notify:"))
async def notification_window_selected(callback: CallbackQuery, state: FSMContext, session: AsyncSession, t, translator):
    code = callback.data.split(":", 1)[1]
    window = next((w for w in NOTIFICATION_WINDOWS if w[0] == code), None)
    if window is None:
        await callback.answer()
        return
    start, end = parse_time_window(window[0])
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        user.notification_window_start = start
        user.notification_window_end = end
        await session.commit()
    await state.set_state(RegistrationStates.ask_temp_registration)
    await callback.message.answer(t("start.ask_temp_registration"))


@router.message(RegistrationStates.ask_temp_registration)
async def temp_registration_answer(message: Message, state: FSMContext, session: AsyncSession, t, language: str):
    text = (message.text or "").lower()
    if "да" in text or "yes" in text:
        await state.set_state(RegistrationStates.ask_temp_registration_date)
        await message.answer(t("start.ask_temp_registration_date"))
        return
    if "нет" in text or "no" in text:
        await message.answer(t("start.no_temp_registration_hint"))
        await message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))
        await state.clear()
        return
    await message.answer(t("errors.invalid_choice"))


@router.message(RegistrationStates.ask_temp_registration_date)
async def temp_registration_date(message: Message, state: FSMContext, session: AsyncSession, t, language: str, translator):
    try:
        expiry = parse_date_by_language(message.text or "", language)
    except ValueError:
        await message.answer(t("errors.invalid_date"))
        return
    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await message.answer(t("errors.general"))
        return
    await ensure_user_documents(session, user)
    doc_row = await session.execute(
        select(UserDocument, DocumentType)
        .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        .where(UserDocument.user_id == user.id, DocumentType.code == "TEMP_REGISTRATION")
    )
    doc = doc_row.scalar_one_or_none()
    if doc:
        doc.current_expiry_date = expiry
        await session.commit()
    await message.answer(t("start.temp_registration_saved", date=translator.format_date(expiry, language)))
    await message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))
    await state.clear()
