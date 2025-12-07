from aiogram import Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import Message
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.services.broadcast_service import BroadcastService
from app.bot.states.user_states import AdminStates
from app.config import load_settings
from app.models.documents import UserDocument
from app.models.user import User

settings = load_settings()
router = Router()


def is_admin(user: User | None) -> bool:
    if user is None:
        return False
    return user.is_admin or user.telegram_id == settings.superadmin_id


@router.message(Command("add_admin"))
async def add_admin(message: Message, session: AsyncSession, state: FSMContext, t):
    requester = (await session.execute(select(User).where(User.telegram_id == message.from_user.id))).scalar_one_or_none()
    if not is_admin(requester):
        await message.answer(t("errors.not_admin"))
        return
    if message.reply_to_message and message.reply_to_message.from_user:
        target_id = message.reply_to_message.from_user.id
        await _set_admin(session, target_id, message, t)
        return
    await state.set_state(AdminStates.add_admin)
    await message.answer(t("admin.ask_user_id"))


@router.message(AdminStates.add_admin)
async def add_admin_by_id(message: Message, session: AsyncSession, state: FSMContext, t):
    try:
        target_id = int(message.text.strip())
    except Exception:
        await message.answer(t("errors.invalid_id"))
        return
    await _set_admin(session, target_id, message, t)
    await state.clear()


async def _set_admin(session: AsyncSession, target_id: int, message: Message, t):
    user = (await session.execute(select(User).where(User.telegram_id == target_id))).scalar_one_or_none()
    if not user:
        await message.answer(t("errors.user_not_found"))
        return
    user.is_admin = True
    await session.commit()
    await message.answer(t("admin.added"))


@router.message(Command("broadcast"))
async def broadcast(message: Message, session: AsyncSession, state: FSMContext, t, bot):
    requester = (await session.execute(select(User).where(User.telegram_id == message.from_user.id))).scalar_one_or_none()
    if not is_admin(requester):
        await message.answer(t("errors.not_admin"))
        return
    await state.set_state(AdminStates.broadcast_text)
    await message.answer(t("admin.broadcast_prompt"))


@router.message(AdminStates.broadcast_text)
async def broadcast_text(message: Message, state: FSMContext, t):
    text = message.text or ""
    await state.set_state(AdminStates.broadcast_confirm)
    await state.update_data(text=text)
    await message.answer(t("admin.broadcast_confirm"))


@router.message(AdminStates.broadcast_confirm)
async def broadcast_confirm(message: Message, state: FSMContext, t, bot):
    if message.text.lower() not in {t("buttons.yes").lower(), "yes", "да"}:
        await message.answer(t("admin.broadcast_cancel"))
        await state.clear()
        return
    data = await state.get_data()
    sessionmaker = getattr(bot, "sessionmaker", None)
    service = BroadcastService(bot, sessionmaker=sessionmaker)
    sent, failed = await service.send_broadcast(data.get("text", ""))
    await message.answer(t("admin.broadcast_done", sent=sent, failed=failed))
    await state.clear()


@router.message(Command("stats"))
async def stats(message: Message, session: AsyncSession, t):
    requester = (await session.execute(select(User).where(User.telegram_id == message.from_user.id))).scalar_one_or_none()
    if not is_admin(requester):
        await message.answer(t("errors.not_admin"))
        return
    count_users = (await session.execute(select(func.count(User.id)))).scalar()
    langs = (await session.execute(select(User.language, func.count(User.id)).group_by(User.language))).all()
    citizenships = (await session.execute(select(User.citizenship_code, func.count(User.id)).group_by(User.citizenship_code))).all()
    active_docs = (
        await session.execute(
            select(func.count(UserDocument.id)).where(UserDocument.notifications_enabled.is_(True))
        )
    ).scalar()
    lines = [t("admin.stats_header", users=count_users, active=active_docs)]
    lines.append(t("admin.stats_languages"))
    for lang, cnt in langs:
        lines.append(f"- {lang or '-'}: {cnt}")
    lines.append(t("admin.stats_citizenships"))
    for code, cnt in citizenships:
        lines.append(f"- {code or '-'}: {cnt}")
    await message.answer("\n".join(lines))
