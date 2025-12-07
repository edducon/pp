from __future__ import annotations

from datetime import time
from typing import Dict, Tuple

from aiogram import Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.inline import (
    citizenship_choices_keyboard,
    citizenship_confirm_keyboard,
    language_keyboard,
    notification_windows_keyboard,
)
from app.bot.keyboards.reply import contact_request_keyboard, main_menu, remove_keyboard
from app.bot.services.citizenship import find_matches
from app.bot.states.user_states import Onboarding
from app.models.user import User

router = Router()

NOTIFICATION_WINDOWS: Dict[str, Tuple[time, time]] = {
    "day": (time(hour=9), time(hour=18)),
    "evening": (time(hour=9), time(hour=22)),
    "noon": (time(hour=12), time(hour=20)),
}


@router.message(Command("start"))
async def cmd_start(message: Message, state: FSMContext, session: AsyncSession, translator) -> None:
    await state.clear()
    user = await _get_or_create_user(session, message)
    await session.commit()
    await message.answer(translator.t("start.choose_language"), reply_markup=language_keyboard())
    await state.set_state(Onboarding.choosing_language)


@router.callback_query(lambda c: c.data and c.data.startswith("lang:"))
async def language_chosen(
    callback: CallbackQuery,
    state: FSMContext,
    session: AsyncSession,
    translator,
    translation_loader,
) -> None:
    _, lang = callback.data.split(":", 1)
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one()
    user.language = lang
    await session.commit()
    translator = translation_loader.get_translator(lang)
    await callback.message.edit_text(translator.t("start.citizenship_prompt"))
    await state.set_state(Onboarding.entering_citizenship)


@router.message(Onboarding.entering_citizenship)
async def process_citizenship(message: Message, state: FSMContext, translator, session: AsyncSession) -> None:
    matches = find_matches(message.text)
    if not matches:
        await message.answer(translator.t("errors.citizenship_not_found"))
        return
    if len(matches) == 1:
        citizenship, _ = matches[0]
        await state.update_data(selected_citizenship=(citizenship.code, citizenship))
        await message.answer(
            translator.t(
                "start.confirm_citizenship", country=citizenship.name_ru if translator.language == "ru" else citizenship.name_en
            ),
            reply_markup=citizenship_confirm_keyboard(
                citizenship.name_ru if translator.language == "ru" else citizenship.name_en,
                citizenship.code,
            ),
        )
        await state.set_state(Onboarding.confirming_citizenship)
    else:
        display = [
            (
                c.code,
                c.name_ru if translator.language == "ru" else c.name_en,
            )
            for c, _ in matches[:5]
        ]
        await message.answer(
            translator.t("start.choose_from_list"),
            reply_markup=citizenship_choices_keyboard(display, translator.t("buttons.none_of_list")),
        )
        await state.set_state(Onboarding.confirming_citizenship)


@router.callback_query(Onboarding.confirming_citizenship, lambda c: c.data and c.data.startswith("citizenship:"))
async def confirm_citizenship(callback: CallbackQuery, state: FSMContext, translator, session: AsyncSession) -> None:
    parts = callback.data.split(":")
    action = parts[1]
    if action == "no":
        await callback.message.answer(translator.t("start.citizenship_retry"))
        await state.set_state(Onboarding.entering_citizenship)
        return
    if action in {"yes", "choose"}:
        code = parts[2]
        name = parts[3]
        result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
        user = result.scalar_one()
        user.citizenship_code = code
        user.citizenship_name = name
        await session.commit()
        await callback.message.answer(
            translator.t("start.ask_phone"),
            reply_markup=contact_request_keyboard(translator.t("buttons.share_phone"), translator.t("buttons.skip")),
        )
        await state.set_state(Onboarding.entering_phone)
    elif action == "no_match":
        await callback.message.answer(translator.t("start.citizenship_retry"))
        await state.set_state(Onboarding.entering_citizenship)


@router.message(Onboarding.entering_phone)
async def process_phone(message: Message, state: FSMContext, translator, session: AsyncSession) -> None:
    text = message.text or ""
    contact = message.contact
    phone = None
    if contact and contact.phone_number:
        phone = contact.phone_number
    elif text and text == translator.t("buttons.skip"):
        phone = None
    elif text:
        phone = text

    if phone and not phone.startswith("+7"):
        await message.answer(translator.t("errors.non_russian_phone"))
        return

    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one()
    user.phone = phone
    await session.commit()

    options = [
        (key, translator.t("start.notification_window", start=times[0].strftime("%H:%M"), end=times[1].strftime("%H:%M")))
        for key, times in NOTIFICATION_WINDOWS.items()
    ]
    await message.answer(translator.t("start.ask_notification_window"), reply_markup=notification_windows_keyboard(options))
    await state.set_state(Onboarding.choosing_window)


@router.callback_query(Onboarding.choosing_window, lambda c: c.data and c.data.startswith("notify_window:"))
async def choose_window(callback: CallbackQuery, state: FSMContext, translator, session: AsyncSession) -> None:
    key = callback.data.split(":", 1)[1]
    start, end = NOTIFICATION_WINDOWS.get(key, (time(hour=9), time(hour=22)))
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one()
    user.notification_window_start = start
    user.notification_window_end = end
    await session.commit()

    await callback.message.answer(translator.t("start.completed"), reply_markup=main_menu())
    await state.clear()


async def _get_or_create_user(session: AsyncSession, message: Message) -> User:
    result = await session.execute(select(User).where(User.telegram_id == message.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        return user
    user = User(
        telegram_id=message.from_user.id,
        username=message.from_user.username,
        language="ru",
    )
    session.add(user)
    return user
