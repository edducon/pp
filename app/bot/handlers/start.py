from __future__ import annotations
from datetime import datetime, timedelta
from typing import Any

from aiogram import F, Router
from aiogram.filters import CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.inline import (
    citizenship_options,
    confirm_keyboard,
    language_keyboard,
    main_menu_keyboard,
    notification_windows_keyboard,
    yes_no_keyboard,
)
from app.bot.keyboards.reply import contact_keyboard, remove_keyboard
from app.bot.services.document_service import ensure_user_documents, parse_time_window
from app.bot.services.country_service import (
    country_rules,
    estimate_entry_date,
    search_countries,
)
from app.bot.states.user_states import RegistrationStates
from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()


NOTIFICATION_WINDOWS = [
    ("09:00-18:00", "09:00–18:00"),
    ("09:00-22:00", "09:00–22:00"),
    ("12:00-20:00", "12:00–20:00"),
]


def notification_window_options(t) -> list[tuple[str, str]]:
    return [
        (code, t("start.notification_window", start=label.split("–")[0], end=label.split("–")[1]))
        for code, label in NOTIFICATION_WINDOWS
    ]


async def prompt_notification_window(target, t) -> None:
    options = notification_window_options(t)
    await target.answer(t("start.ask_notifications_window"), reply_markup=notification_windows_keyboard(options))


async def get_or_create_user(session: AsyncSession, telegram_id: int, username: str | None) -> User:
    result = await session.execute(select(User).where(User.telegram_id == telegram_id))
    user = result.scalar_one_or_none()
    if user is None:
        user = User(telegram_id=telegram_id, username=username)
        session.add(user)
        await session.commit()
    await ensure_user_documents(session, user)
    return user


def parse_date_by_language(value: str, language: str) -> datetime.date:
    fmt = "%d.%m.%Y" if language.startswith("ru") else "%Y-%m-%d"
    return datetime.strptime(value.strip(), fmt).date()


async def get_user_document_by_code(session: AsyncSession, user: User, code: str) -> UserDocument | None:
    await ensure_user_documents(session, user)
    doc_row = await session.execute(
        select(UserDocument)
        .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        .where(UserDocument.user_id == user.id, DocumentType.code == code)
    )
    return doc_row.scalar_one_or_none()


async def user_has_completed_registration(session: AsyncSession, user: User) -> bool:
    migration_card = await get_user_document_by_code(session, user, "MIGRATION_CARD")
    return bool(
        user.citizenship_code
        and user.phone
        and migration_card
        and migration_card.current_expiry_date
    )


@router.message(CommandStart())
async def cmd_start(message: Message, state: FSMContext, session: AsyncSession, t, translator, language: str):
    user = await get_or_create_user(session, message.from_user.id, message.from_user.username)
    if await user_has_completed_registration(session, user):
        await state.clear()
        await message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))
        return
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
    matches = search_countries(message.text or "")
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
    await callback.message.edit_reply_markup()
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
    await state.set_state(RegistrationStates.ask_migration_card_expiry)
    await message.answer(t("start.ask_migration_card_expiry"), reply_markup=remove_keyboard())


@router.message(RegistrationStates.ask_migration_card_expiry)
async def save_migration_card_expiry(
    message: Message, state: FSMContext, session: AsyncSession, t, translator, language: str
):
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

    migration_card = await get_user_document_by_code(session, user, "MIGRATION_CARD")
    if not migration_card:
        await message.answer(t("errors.general"))
        return

    migration_card.current_expiry_date = expiry
    await session.commit()

    await state.set_state(RegistrationStates.ask_temp_registration)
    await message.answer(t("start.migration_card_saved", date=translator.format_date(expiry, language)))
    await message.answer(
        t("start.ask_temp_registration"),
        reply_markup=yes_no_keyboard("tempreg:yes", "tempreg:no"),
    )


@router.callback_query(F.data.startswith("notify:"))
async def notification_window_selected(callback: CallbackQuery, state: FSMContext, session: AsyncSession, t, translator):
    code = callback.data.split(":", 1)[1]
    window = next((w for w in NOTIFICATION_WINDOWS if w[0] == code), None)
    if window is None:
        await callback.answer()
        return
    await callback.message.edit_reply_markup()
    start, end = parse_time_window(window[0])
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if user:
        user.notification_window_start = start
        user.notification_window_end = end
        await session.commit()
    start_label, end_label = window[1].split("–")
    await callback.message.answer(t("start.notification_window", start=start_label, end=end_label))
    await callback.message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))
    await state.clear()


@router.callback_query(RegistrationStates.ask_temp_registration, F.data.startswith("tempreg:"))
async def temp_registration_answer(
    callback: CallbackQuery, state: FSMContext, session: AsyncSession, t, language: str, translator
):
    answer = callback.data.split(":", 1)[1]
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await callback.message.answer(t("errors.general"))
        return

    if answer == "yes":
        await state.set_state(RegistrationStates.ask_temp_registration_date)
        await callback.message.edit_text(t("start.ask_temp_registration_date"))
        await callback.answer()
        return

    migration_card = await get_user_document_by_code(session, user, "MIGRATION_CARD")
    if not migration_card or not migration_card.current_expiry_date:
        await callback.message.answer(t("errors.general"))
        return

    temp_registration_doc = await get_user_document_by_code(session, user, "TEMP_REGISTRATION")
    if not temp_registration_doc:
        await callback.message.answer(t("errors.general"))
        return

    rules = country_rules(callback.bot.settings, user.citizenship_code)
    entry_date = estimate_entry_date(migration_card.current_expiry_date, rules)
    auto_expiry = entry_date + timedelta(days=rules.registration_days)
    temp_registration_doc.current_expiry_date = auto_expiry
    await session.commit()

    await state.set_state(RegistrationStates.ask_medical_exam)
    await callback.message.edit_text(
        t(
            "start.temp_registration_autoset",
            date=translator.format_date(auto_expiry, language),
            days=rules.registration_days,
        )
    )
    await callback.message.answer(
        t("start.ask_medical_exam"), reply_markup=yes_no_keyboard("medexam:yes", "medexam:no")
    )
    await callback.answer()


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
    await state.set_state(RegistrationStates.ask_medical_exam)
    await message.answer(t("start.ask_medical_exam"), reply_markup=yes_no_keyboard("medexam:yes", "medexam:no"))


@router.message(RegistrationStates.ask_temp_registration)
async def temp_registration_buttons_hint(message: Message, t):
    await message.answer(t("errors.invalid_choice"), reply_markup=yes_no_keyboard("tempreg:yes", "tempreg:no"))


@router.callback_query(RegistrationStates.ask_medical_exam, F.data.startswith("medexam:"))
async def medical_exam_answer(
    callback: CallbackQuery, state: FSMContext, session: AsyncSession, t, language: str, translator
):
    answer = callback.data.split(":", 1)[1]
    result = await session.execute(select(User).where(User.telegram_id == callback.from_user.id))
    user = result.scalar_one_or_none()
    if not user:
        await callback.message.answer(t("errors.general"))
        return

    if answer == "yes":
        await state.set_state(RegistrationStates.ask_med_cert_1_date)
        await callback.message.edit_text(t("start.ask_med_cert_1_date"))
        await callback.answer()
        return

    migration_card = await get_user_document_by_code(session, user, "MIGRATION_CARD")
    if not migration_card or not migration_card.current_expiry_date:
        await callback.message.answer(t("errors.general"))
        return

    rules = country_rules(callback.bot.settings, user.citizenship_code)
    entry_date = estimate_entry_date(migration_card.current_expiry_date, rules)
    deadline = entry_date + timedelta(days=rules.medical_days)
    med_codes = ["MED_CERT_1", "MED_CERT_2", "MED_CERT_3"]
    for code in med_codes:
        doc = await get_user_document_by_code(session, user, code)
        if doc:
            doc.current_expiry_date = deadline
    await session.commit()

    await callback.message.edit_text(
        t(
            "start.medical_exam_deadline",
            date=translator.format_date(deadline, language),
            days=rules.medical_days,
        )
    )
    await state.set_state(RegistrationStates.choose_notification_window)
    await prompt_notification_window(callback.message, t)
    await callback.answer()


@router.message(RegistrationStates.ask_medical_exam)
async def medical_exam_buttons_hint(message: Message, t):
    await message.answer(t("errors.invalid_choice"), reply_markup=yes_no_keyboard("medexam:yes", "medexam:no"))


async def _save_med_cert_date(
    message: Message,
    state: FSMContext,
    session: AsyncSession,
    t,
    language: str,
    translator,
    code: str,
    next_state,
    next_prompt_key: str | None,
):
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

    doc = await get_user_document_by_code(session, user, code)
    if not doc:
        await message.answer(t("errors.general"))
        return

    doc.current_expiry_date = expiry
    await session.commit()

    await message.answer(t("start.med_cert_saved", date=translator.format_date(expiry, language)))
    if next_state:
        await state.set_state(next_state)
        if next_prompt_key:
            await message.answer(t(next_prompt_key))
    else:
        await state.set_state(RegistrationStates.choose_notification_window)
        await prompt_notification_window(message, t)


@router.message(RegistrationStates.ask_med_cert_1_date)
async def save_med_cert_1_date(
    message: Message, state: FSMContext, session: AsyncSession, t, language: str, translator
):
    await _save_med_cert_date(
        message,
        state,
        session,
        t,
        language,
        translator,
        "MED_CERT_1",
        RegistrationStates.ask_med_cert_2_date,
        "start.ask_med_cert_2_date",
    )


@router.message(RegistrationStates.ask_med_cert_2_date)
async def save_med_cert_2_date(
    message: Message, state: FSMContext, session: AsyncSession, t, language: str, translator
):
    await _save_med_cert_date(
        message,
        state,
        session,
        t,
        language,
        translator,
        "MED_CERT_2",
        RegistrationStates.ask_med_cert_3_date,
        "start.ask_med_cert_3_date",
    )


@router.message(RegistrationStates.ask_med_cert_3_date)
async def save_med_cert_3_date(
    message: Message, state: FSMContext, session: AsyncSession, t, language: str, translator
):
    await _save_med_cert_date(
        message,
        state,
        session,
        t,
        language,
        translator,
        "MED_CERT_3",
        None,
        None,
    )
