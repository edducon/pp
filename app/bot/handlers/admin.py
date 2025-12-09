from __future__ import annotations

import datetime as dt

from aiogram import Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import and_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.keyboards.admin import admin_profile_keyboard, admin_user_list_keyboard
from app.bot.keyboards.common import travel_keyboard
from app.bot.states import AdminBroadcastState
from app.models import Admin, MigrationCard, User
from app.services.migration_cards import MigrationCardService

router = Router()


async def _is_admin(user_id: int, session: AsyncSession, superadmin_id: int) -> bool:
    if user_id == superadmin_id:
        return True
    stmt = select(Admin).join(User).where(User.telegram_id == user_id)
    return await session.scalar(stmt) is not None


@router.message(Command("add_admin"))
async def add_admin(message: Message, session: AsyncSession, t, locale, settings):
    if message.from_user.id != settings.superadmin_id:
        await message.answer(t("admin.access_denied", locale))
        return
    try:
        target_id = int(message.text.split(maxsplit=1)[1])
    except Exception:
        await message.answer("Usage: /add_admin <telegram_id>")
        return
    user = await session.scalar(select(User).where(User.telegram_id == target_id))
    if not user:
        await message.answer("User not registered")
        return
    session.add(Admin(user_id=user.id, granted_by=message.from_user.id))
    await session.commit()
    await message.answer(t("admin.added", locale))


@router.message(Command("notify"))
async def admin_notify(message: Message, state: FSMContext, session: AsyncSession, t, locale, settings):
    if not await _is_admin(message.from_user.id, session, settings.superadmin_id):
        await message.answer(t("admin.access_denied", locale))
        return
    await state.set_state(AdminBroadcastState.waiting_for_dates)
    await message.answer(t("admin.broadcast_dates", locale))


@router.message(AdminBroadcastState.waiting_for_dates)
async def notify_dates(message: Message, state: FSMContext, t, locale):
    try:
        date_from_str, date_to_str = message.text.split("-")
        date_from = dt.datetime.strptime(date_from_str.strip(), "%d.%m.%Y").date()
        date_to = dt.datetime.strptime(date_to_str.strip(), "%d.%m.%Y").date()
    except Exception:
        await message.answer(t("admin.broadcast_dates", locale))
        return
    await state.update_data(date_from=date_from, date_to=date_to)
    await state.set_state(AdminBroadcastState.waiting_for_message)
    await message.answer(t("admin.broadcast_message", locale))


@router.message(AdminBroadcastState.waiting_for_message)
async def notify_message(message: Message, state: FSMContext, session: AsyncSession, t, locale):
    data = await state.get_data()
    date_from: dt.date = data["date_from"]
    date_to: dt.date = data["date_to"]
    stmt = select(User.telegram_id, User.language).join(MigrationCard).where(
        and_(MigrationCard.expires_at >= date_from, MigrationCard.expires_at <= date_to)
    )
    users = (await session.execute(stmt)).all()
    for chat_id, lang in users:
        await message.bot.send_message(chat_id, message.text, parse_mode=None)
    await state.clear()
    await message.answer(t("admin.broadcast_done", locale))


@router.message(Command("wants"))
async def list_users(message: Message, session: AsyncSession, t, locale, settings):
    if not await _is_admin(message.from_user.id, session, settings.superadmin_id):
        await message.answer(t("admin.access_denied", locale))
        return
    await _send_user_page(message, session, locale, t, page=1)


@router.callback_query(lambda c: c.data and c.data.startswith("page:"))
async def paginate(callback: CallbackQuery, session: AsyncSession, t, locale):
    page = int(callback.data.split(":", 1)[1])
    await _send_user_page(callback.message, session, locale, t, page=page)
    await callback.answer()


async def _send_user_page(message: Message, session: AsyncSession, locale: str, t, page: int = 1, page_size: int = 5):
    stmt = select(User).offset((page - 1) * page_size).limit(page_size)
    rows = (await session.scalars(stmt)).all()
    buttons = [(f"{row.last_name} {row.first_name}", row.id) for row in rows]
    kb = admin_user_list_keyboard(buttons, page=page)
    await message.answer(t("admin.user_list", locale, page=page), reply_markup=kb)


@router.callback_query(lambda c: c.data and c.data.startswith("user:"))
async def open_user(callback: CallbackQuery, session: AsyncSession, t, locale):
    user_id = int(callback.data.split(":", 1)[1])
    user = await session.get(User, user_id)
    card = await session.scalar(select(MigrationCard).where(MigrationCard.user_id == user_id))
    if not user or not card:
        await callback.answer()
        return
    phone = user.phone_from_telegram or user.manual_phone or "-"
    citizenship = user.citizenship.name_ru if user.citizenship else "-"
    kb = admin_profile_keyboard(user_id=user_id, card_id=card.id, label=t("admin.mark_extended", locale))
    await callback.message.answer(
        t(
            "admin.profile",
            locale,
            name=f"{user.last_name} {user.first_name}",
            citizenship=citizenship,
            phone=phone,
            date=card.expires_at.strftime("%d.%m.%Y"),
        ),
        reply_markup=kb,
    )
    await callback.answer()


@router.callback_query(lambda c: c.data and c.data.startswith("admin_mark:"))
async def mark_extended(callback: CallbackQuery, session: AsyncSession, t, locale):
    _, user_id, card_id = callback.data.split(":", 2)
    card = await session.get(MigrationCard, int(card_id))
    if not card:
        await callback.answer()
        return
    service = MigrationCardService(session)
    await service.mark_extended_by_admin(card, note="admin_marked")
    await callback.message.answer(t("notifications.admin_extended", locale))
    user = await session.get(User, card.user_id)
    user_locale = user.language if user else locale
    await callback.bot.send_message(card.user.telegram_id, t("notifications.admin_extended", user_locale))
    kb = travel_keyboard(
        t("notifications.travel_yes", user_locale),
        t("notifications.travel_no", user_locale),
        card.id,
    )
    await callback.bot.send_message(card.user.telegram_id, t("notifications.travel_check", user_locale), reply_markup=kb)
    await session.commit()
    await callback.answer()
