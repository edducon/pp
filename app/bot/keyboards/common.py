from __future__ import annotations

from aiogram.types import InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup


def language_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="Русский", callback_data="lang:ru")],
            [InlineKeyboardButton(text="English", callback_data="lang:en")],
        ]
    )


def phone_keyboard(request_label: str, skip_label: str) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text=request_label, request_contact=True)],
            [KeyboardButton(text=skip_label)],
        ],
        resize_keyboard=True,
        one_time_keyboard=True,
    )


def citizenship_keyboard(options: list[tuple[str, int]], retry_label: str) -> InlineKeyboardMarkup:
    buttons = [[InlineKeyboardButton(text=label, callback_data=f"cit:{cid}")] for label, cid in options]
    buttons.append([InlineKeyboardButton(text=retry_label, callback_data="cit:retry")])
    return InlineKeyboardMarkup(inline_keyboard=buttons)


def pause_keyboard(button_text: str, card_id: int) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[[InlineKeyboardButton(text=button_text, callback_data=f"pause_card:{card_id}")]]
    )


def travel_keyboard(yes: str, no: str, card_id: int) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text=yes, callback_data=f"travel:{card_id}:yes")],
            [InlineKeyboardButton(text=no, callback_data=f"travel:{card_id}:no")],
        ]
    )
