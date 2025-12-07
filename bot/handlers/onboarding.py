from __future__ import annotations

from datetime import time

from aiogram import F, Router
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.fsm.state import State, StatesGroup
from aiogram.types import CallbackQuery, Message

from bot.db import get_session
from bot.handlers.keyboards import (
    cancel_keyboard,
    contact_keyboard,
    countries_keyboard,
    language_keyboard,
    main_menu_keyboard,
    notification_window_keyboard,
)
from bot.i18n.loader import TranslationLoader
from bot.services.citizenship import match_countries
from bot.services.document_service import DocumentService
from bot.services.user_service import UserService

router = Router()


class OnboardingStates(StatesGroup):
    language = State()
    citizenship = State()
    phone = State()
    notification_window = State()


def parse_window(value: str) -> tuple[time, time]:
    start_str, end_str = value.split("-")
    start_h, start_m = map(int, start_str.split(":"))
    end_h, end_m = map(int, end_str.split(":"))
    return time(hour=start_h, minute=start_m), time(hour=end_h, minute=end_m)


@router.message(CommandStart())
async def start_command(message: Message, state: FSMContext, translation_loader: TranslationLoader, t) -> None:
    await state.clear()
    async with get_session() as session:
        user_service = UserService(session)
        existing = await user_service.get_by_telegram(message.from_user.id)
        if existing:
            await message.answer(t("onboarding.already_registered"), reply_markup=main_menu_keyboard(t))
            return
    await state.set_state(OnboardingStates.language)
    await message.answer(t("onboarding.choose_language"), reply_markup=language_keyboard(t))


@router.callback_query(F.data.startswith("lang:"), OnboardingStates.language)
async def select_language(callback: CallbackQuery, state: FSMContext, translation_loader: TranslationLoader, t) -> None:
    lang = callback.data.split(":")[1]
    translator = translation_loader.get_translator(lang)
    await state.update_data(language=lang)
    await state.set_state(OnboardingStates.citizenship)
    await callback.message.edit_text(translator.t("onboarding.enter_citizenship"))


@router.message(OnboardingStates.citizenship)
async def citizenship_input(message: Message, state: FSMContext, translation_loader: TranslationLoader) -> None:
    data = await state.get_data()
    lang = data.get("language", "ru")
    translator = translation_loader.get_translator(lang)
    matches, suggestion = match_countries(message.text)
    if not matches:
        if suggestion:
            await message.answer(
                translator.t(
                    "onboarding.citizenship_suggestion",
                    query=message.text,
                    suggestion=suggestion.display_name,
                ),
                reply_markup=countries_keyboard([suggestion]),
            )
        else:
            await message.answer(translator.t("onboarding.citizenship_not_found"), reply_markup=cancel_keyboard(translator.t))
        return
    await state.update_data(countries=[c.code for c in matches])
    await message.answer(translator.t("onboarding.choose_from_list"), reply_markup=countries_keyboard(matches))


@router.callback_query(F.data.startswith("cit:"), OnboardingStates.citizenship)
async def citizenship_selected(callback: CallbackQuery, state: FSMContext, translation_loader: TranslationLoader) -> None:
    code = callback.data.split(":")[1]
    data = await state.get_data()
    lang = data.get("language", "ru")
    translator = translation_loader.get_translator(lang)
    await state.update_data(citizenship=code)
    await state.set_state(OnboardingStates.phone)
    await callback.message.answer(translator.t("onboarding.ask_phone"), reply_markup=contact_keyboard(translator.t))


@router.message(F.contact, OnboardingStates.phone)
async def phone_shared(message: Message, state: FSMContext, translation_loader: TranslationLoader) -> None:
    data = await state.get_data()
    lang = data.get("language", "ru")
    translator = translation_loader.get_translator(lang)
    phone = message.contact.phone_number
    if not phone.startswith("+7"):
        await message.answer(translator.t("onboarding.invalid_phone"))
        return
    await state.update_data(phone=phone)
    await state.set_state(OnboardingStates.notification_window)
    await message.answer(translator.t("onboarding.time_window"), reply_markup=notification_window_keyboard(translator.t))


@router.message(OnboardingStates.phone)
async def phone_text(message: Message, state: FSMContext, translation_loader: TranslationLoader) -> None:
    data = await state.get_data()
    lang = data.get("language", "ru")
    translator = translation_loader.get_translator(lang)
    if message.text == translator.t("actions.cancel"):
        await state.clear()
        await message.answer(translator.t("actions.cancelled"), reply_markup=main_menu_keyboard(translator.t))
        return
    digits = message.text.replace(" ", "")
    if not digits.startswith("+7"):
        await message.answer(translator.t("onboarding.invalid_phone"))
        return
    await state.update_data(phone=digits)
    await state.set_state(OnboardingStates.notification_window)
    await message.answer(translator.t("onboarding.time_window"), reply_markup=notification_window_keyboard(translator.t))


@router.callback_query(F.data.startswith("win:"), OnboardingStates.notification_window)
async def window_selected(callback: CallbackQuery, state: FSMContext, translation_loader: TranslationLoader) -> None:
    data = await state.get_data()
    lang = data.get("language", "ru")
    translator = translation_loader.get_translator(lang)
    window_value = callback.data.split(":")[1]
    start, end = parse_window(window_value)
    await state.update_data(notification_window_start=start, notification_window_end=end)

    async with get_session() as session:
        user_service = UserService(session)
        doc_service = DocumentService(session)
        user = await user_service.create_user(
            telegram_id=callback.from_user.id,
            language=lang,
            citizenship_code=data["citizenship"],
            phone=data.get("phone"),
            window_start=start,
            window_end=end,
        )
        await doc_service.ensure_document_types()
        await doc_service.bootstrap_user_documents(user)
        await session.commit()

    await state.clear()
    await callback.message.edit_text(translator.t("onboarding.completed"), reply_markup=main_menu_keyboard(translator.t))
