from aiogram import F, Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.bot.handlers.start import language_keyboard
from app.bot.keyboards.inline import yes_no_keyboard
from app.bot.keyboards.reply import main_menu_keyboard
from app.bot.states.user_states import RegistrationStates
from app.models.documents import UserDocument
from app.models.user import User

router = Router()


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
    await message.answer(t("menu.ready"), reply_markup=main_menu_keyboard(t))
