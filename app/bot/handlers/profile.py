from datetime import date

from aiogram import F, Router
from aiogram.exceptions import TelegramBadRequest
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, InlineKeyboardButton, InlineKeyboardMarkup, Message
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.handlers.documents import doc_display_name
from app.bot.handlers.start import language_keyboard
from app.bot.keyboards.inline import main_menu_keyboard, menu_back_keyboard, yes_no_keyboard
from app.bot.states.user_states import RegistrationStates
from app.models.documents import DocumentType, UserDocument
from app.models.user import User

router = Router()

B103_MAP_URL = "https://yandex.ru/maps/?text=%D0%91%D0%A1103"


async def show_inline_menu(message: Message, t):
    await message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))


async def render_documents_overview(session: AsyncSession, user_id: int, t, language: str, translator):
    result = await session.execute(
        select(UserDocument, DocumentType)
        .join(DocumentType, DocumentType.id == UserDocument.document_type_id)
        .join(User, User.id == UserDocument.user_id)
        .where(User.telegram_id == user_id)
    )
    docs = result.all()
    if not docs:
        return t("documents.none"), False

    rows = []
    has_expired = False
    today = date.today()
    for doc, doc_type in docs:
        expiry_text = translator.format_date(doc.current_expiry_date, language) if doc.current_expiry_date else t("status.empty")
        rows.append(
            t(
                "status.item",
                doc_name=doc_display_name(doc_type, language),
                expiry=expiry_text,
                notif=t("buttons.on") if doc.notifications_enabled else t("buttons.off"),
            )
        )
        if doc.current_expiry_date and doc.current_expiry_date < today:
            has_expired = True

    return "\n".join([t("status.header"), *rows, t("menu.exit_check")]), has_expired


@router.message(Command("language"))
async def change_language(message: Message, state: FSMContext, t):
    await state.set_state(RegistrationStates.choose_language)
    await message.answer(t("start.choose_language"), reply_markup=language_keyboard())


@router.message(Command("help"))
async def help_command(message: Message, t):
    await message.answer(t("help.text"))


@router.message(Command("delete_me"))
async def delete_me(message: Message, t):
    await message.answer(t("delete.confirm"), reply_markup=yes_no_keyboard("delete:yes", "delete:no"))


@router.callback_query(F.data.startswith("delete:"))
async def delete_confirm(callback: CallbackQuery, session: AsyncSession, t):
    if callback.data.endswith("no"):
        await callback.message.answer(t("delete.cancel"), reply_markup=main_menu_keyboard(t))
        return
    user = (await session.execute(select(User).where(User.telegram_id == callback.from_user.id))).scalar_one_or_none()
    if user:
        await session.execute(delete(UserDocument).where(UserDocument.user_id == user.id))
        await session.delete(user)
        await session.commit()
    await callback.message.answer(t("delete.done"))


@router.message(Command("menu"))
async def show_menu(message: Message, t):
    await show_inline_menu(message, t)


@router.callback_query(F.data == "menu:root")
async def menu_root(callback: CallbackQuery, t):
    await callback.message.edit_text(t("menu.ready"), reply_markup=main_menu_keyboard(t))
    await callback.answer()


def documents_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="↻", callback_data="menu:documents")],
            [InlineKeyboardButton(text="⬅️", callback_data="menu:root")],
        ]
    )


@router.callback_query(F.data == "menu:documents")
async def menu_documents(callback: CallbackQuery, session: AsyncSession, t, language: str, translator):
    text, has_expired = await render_documents_overview(session, callback.from_user.id, t, language, translator)
    answered = False
    if callback.message.text == text:
        await callback.answer(t("menu.no_changes"))
        answered = True
    else:
        try:
            await callback.message.edit_text(text, reply_markup=documents_keyboard())
        except TelegramBadRequest:
            await callback.answer(t("menu.no_changes"))
            answered = True
        else:
            await callback.answer(t("menu.updated"))
            answered = True
    if has_expired:
        await callback.message.answer(
            t("alerts.visit_b103"),
            reply_markup=InlineKeyboardMarkup(
                inline_keyboard=[[InlineKeyboardButton(text=t("alerts.map"), url=B103_MAP_URL)]]
            ),
        )
    if not answered:
        await callback.answer()


@router.callback_query(F.data == "menu:help")
async def menu_help(callback: CallbackQuery, t):
    await callback.message.edit_text(t("help.text"), reply_markup=menu_back_keyboard())
    await callback.answer()


@router.callback_query(F.data == "menu:settings")
async def menu_settings(callback: CallbackQuery, t):
    await callback.message.edit_text(t("menu.settings_hint"), reply_markup=menu_back_keyboard())
    await callback.answer()
