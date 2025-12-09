from __future__ import annotations

from aiogram.types import InlineKeyboardButton, InlineKeyboardMarkup


def admin_user_list_keyboard(users: list[tuple[str, int]], page: int) -> InlineKeyboardMarkup:
    rows = [[InlineKeyboardButton(text=title, callback_data=f"user:{user_id}")] for title, user_id in users]
    navigation = [InlineKeyboardButton(text="◀️", callback_data=f"page:{max(page-1,1)}"), InlineKeyboardButton(text="▶️", callback_data=f"page:{page+1}")]
    rows.append(navigation)
    return InlineKeyboardMarkup(inline_keyboard=rows)


def admin_profile_keyboard(user_id: int, card_id: int, label: str) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[[InlineKeyboardButton(text=label, callback_data=f"admin_mark:{user_id}:{card_id}")]]
    )
