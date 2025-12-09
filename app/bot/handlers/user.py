from __future__ import annotations

import datetime as dt
from typing import Optional

from aiogram import Router
from aiogram.filters import Command, CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.common import citizenship_keyboard, language_keyboard, phone_keyboard, travel_keyboard
from app.bot.states import RegistrationState
from app.enums import MigrationEvent
from app.models import MigrationCard, User
from app.services.countries import CountryService
from app.services.migration_cards import MigrationCardService

router = Router()
country_service: CountryService | None = None


def _preferred_locale(data: dict, fallback: str) -> str:
    return data.get("language", fallback)


def setup_country_service(service: CountryService) -> None:
    global country_service
    country_service = service


@router.message(CommandStart())
async def start(message: Message, state: FSMContext, t, locale):
    await state.set_state(RegistrationState.choosing_language)
    await message.answer(t("start.welcome", locale), reply_markup=language_keyboard())


@router.callback_query(lambda c: c.data and c.data.startswith("lang:"))
async def choose_language(callback: CallbackQuery, state: FSMContext, t, locale):
    lang = callback.data.split(":", 1)[1]
    await state.update_data(language=lang)
    await callback.message.edit_reply_markup()
    await state.set_state(RegistrationState.collecting_first_name)
    await callback.message.answer(t("profile.ask_first_name", lang))


@router.message(RegistrationState.collecting_first_name)
async def collect_first_name(message: Message, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    await state.update_data(first_name=message.text.strip())
    await state.set_state(RegistrationState.collecting_last_name)
    await message.answer(t("profile.ask_last_name", lang))


@router.message(RegistrationState.collecting_last_name)
async def collect_last_name(message: Message, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    await state.update_data(last_name=message.text.strip())
    await state.set_state(RegistrationState.collecting_patronymic)
    await message.answer(t("profile.ask_patronymic", lang))


@router.message(RegistrationState.collecting_patronymic)
async def collect_patronymic(message: Message, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    patronymic = message.text.strip() if message.text else None
    if patronymic and patronymic.lower() in {"skip", "пропустить"}:
        patronymic = None
    await state.update_data(patronymic=patronymic)
    await state.set_state(RegistrationState.collecting_citizenship)
    await message.answer(t("profile.ask_citizenship", lang))


@router.message(RegistrationState.collecting_citizenship)
async def ask_citizenship(message: Message, state: FSMContext, session: AsyncSession, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    if not country_service:
        await message.answer("Dataset not loaded")
        return
    matches = await country_service.search(message.text, session)
    if not matches:
        await message.answer(t("profile.citizenship_not_found", lang))
        return
    buttons = [(f"{m.country.name_ru} / {m.country.name_en}", m.country.id) for m in matches]
    kb = citizenship_keyboard(buttons, retry_label=t("profile.citizenship_not_found", lang))
    await message.answer(t("profile.citizenship_suggestion", lang, country=buttons[0][0]), reply_markup=kb)


@router.callback_query(lambda c: c.data and c.data.startswith("cit:"))
async def choose_citizenship(callback: CallbackQuery, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    choice = callback.data.split(":", 1)[1]
    if choice == "retry":
        await callback.message.answer(t("profile.ask_citizenship", lang))
        return
    country_id = int(choice)
    await state.update_data(citizenship_id=country_id)
    await state.set_state(RegistrationState.sharing_phone)
    kb = phone_keyboard(t("profile.request_contact", lang), t("profile.skip", lang))
    await callback.message.answer(t("profile.ask_phone", lang), reply_markup=kb)


@router.message(RegistrationState.sharing_phone)
async def collect_phone(message: Message, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    phone = None
    manual_needed = False
    if message.contact:
        phone = message.contact.phone_number
        if not phone.startswith("+7"):
            manual_needed = True
    elif message.text and message.text.lower() in {t("profile.skip", lang).lower(), "skip", "пропустить"}:
        manual_needed = False
    else:
        manual_needed = True
    if manual_needed:
        await state.set_state(RegistrationState.manual_phone)
        await message.answer(t("profile.enter_phone", lang))
    else:
        await state.update_data(phone=phone)
        await state.set_state(RegistrationState.collecting_expiry)
        await message.answer(t("profile.ask_expiry", lang))


@router.message(RegistrationState.manual_phone)
async def manual_phone(message: Message, state: FSMContext, t, locale):
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    if message.text and message.text.lower() in {t("profile.skip", lang).lower(), "skip", "пропустить"}:
        phone = None
    else:
        phone = message.text.strip()
    await state.update_data(phone=phone)
    await state.set_state(RegistrationState.collecting_expiry)
    await message.answer(t("profile.phone_saved", lang))
    await message.answer(t("profile.ask_expiry", lang))


def _parse_date(text: str) -> Optional[dt.date]:
    try:
        return dt.datetime.strptime(text.strip(), "%d.%m.%Y").date()
    except ValueError:
        return None


@router.message(RegistrationState.collecting_expiry)
async def save_profile(message: Message, state: FSMContext, session: AsyncSession, t, locale):
    expiry = _parse_date(message.text or "")
    if not expiry:
        data = await state.get_data()
        lang = _preferred_locale(data, locale)
        await message.answer(t("profile.ask_expiry", lang))
        return
    data = await state.get_data()
    lang = _preferred_locale(data, locale)
    user = await _upsert_user(message.from_user.id, data, session)
    card = await session.scalar(select(MigrationCard).where(MigrationCard.user_id == user.id))
    service = MigrationCardService(session)
    if card:
        card = await service.update_expiry(card, expiry, MigrationEvent.EXTENDED)
    else:
        card = await service.create(user.id, expiry)
    await state.clear()
    await message.answer(t("profile.saved", lang))
    if card.needs_travel_confirmation:
        await send_travel_prompt(message, card, t, lang)


async def _upsert_user(telegram_id: int, data: dict, session: AsyncSession) -> User:
    existing: User | None = await session.scalar(select(User).where(User.telegram_id == telegram_id))
    if existing:
        existing.language = data.get("language", existing.language)
        existing.first_name = data.get("first_name", existing.first_name)
        existing.last_name = data.get("last_name", existing.last_name)
        existing.patronymic = data.get("patronymic")
        existing.citizenship_id = data.get("citizenship_id")
        phone = data.get("phone")
        if phone:
            if phone.startswith("+7"):
                existing.phone_from_telegram = phone
            else:
                existing.manual_phone = phone
        return existing
    phone = data.get("phone")
    user = User(
        telegram_id=telegram_id,
        language=data.get("language", "ru"),
        first_name=data["first_name"],
        last_name=data["last_name"],
        patronymic=data.get("patronymic"),
        citizenship_id=data.get("citizenship_id"),
        phone_from_telegram=phone if phone and phone.startswith("+7") else None,
        manual_phone=phone if phone and not phone.startswith("+7") else None,
    )
    session.add(user)
    await session.flush()
    return user


@router.callback_query(lambda c: c.data and c.data.startswith("pause_card:"))
async def pause_notifications(callback: CallbackQuery, session: AsyncSession, t, locale):
    card_id = int(callback.data.split(":", 1)[1])
    card = await session.get(MigrationCard, card_id)
    if not card:
        await callback.answer()
        return
    service = MigrationCardService(session)
    await service.pause_by_user(card, note="user_clicked_pause")
    await callback.message.answer(t("notifications.paused", locale))
    await session.commit()
    await callback.answer()


@router.message(Command("renew"))
async def renew(message: Message, state: FSMContext, session: AsyncSession, t, locale):
    user = await session.scalar(select(User).where(User.telegram_id == message.from_user.id))
    if not user:
        await message.answer(t("start.welcome", locale))
        return
    await state.update_data(
        language=user.language,
        first_name=user.first_name,
        last_name=user.last_name,
        patronymic=user.patronymic,
        citizenship_id=user.citizenship_id,
        phone=user.phone_from_telegram or user.manual_phone,
    )
    await state.set_state(RegistrationState.collecting_expiry)
    await message.answer(t("commands.renew", locale))


@router.callback_query(lambda c: c.data and c.data.startswith("travel:"))
async def travel_confirmation(callback: CallbackQuery, session: AsyncSession, t, locale):
    _, card_id, answer = callback.data.split(":", 2)
    card = await session.get(MigrationCard, int(card_id))
    if not card:
        await callback.answer()
        return
    service = MigrationCardService(session)
    await service.travel_confirmation(card, in_russia=answer == "no")
    await callback.message.answer(t("notifications.travel_recorded", locale))
    await session.commit()
    await callback.answer()


def travel_prompt_text(card: MigrationCard, locale: str, t) -> str:
    return t("notifications.travel_check", locale)


async def send_travel_prompt(message: Message, card: MigrationCard, t, locale):
    kb = travel_keyboard(t("notifications.travel_yes", locale), t("notifications.travel_no", locale), card.id)
    await message.answer(travel_prompt_text(card, locale, t), reply_markup=kb)
