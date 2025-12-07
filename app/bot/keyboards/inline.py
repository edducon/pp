from __future__ import annotations

from typing import Iterable, List, Tuple

from aiogram.types import InlineKeyboardButton, InlineKeyboardMarkup


def language_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="Русский", callback_data="lang:ru"), InlineKeyboardButton(text="English", callback_data="lang:en")]
        ]
    )


def citizenship_confirm_keyboard(country: str, code: str) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="✓", callback_data=f"citizenship:yes:{code}:{country}")],
            [InlineKeyboardButton(text="✕", callback_data="citizenship:no")],
        ]
    )


def citizenship_choices_keyboard(choices: Iterable[Tuple[str, str]]) -> InlineKeyboardMarkup:
    rows: List[List[InlineKeyboardButton]] = []
    for code, name in choices:
        rows.append([InlineKeyboardButton(text=name, callback_data=f"citizenship:choose:{code}:{name}")])
    rows.append([InlineKeyboardButton(text="Ничего из списка", callback_data="citizenship:no_match")])
    return InlineKeyboardMarkup(inline_keyboard=rows)


def notification_windows_keyboard(options: Iterable[Tuple[str, str]]) -> InlineKeyboardMarkup:
    rows = []
    for key, label in options:
        rows.append([InlineKeyboardButton(text=label, callback_data=f"notify_window:{key}")])
    return InlineKeyboardMarkup(inline_keyboard=rows)


def yes_no_keyboard(prefix: str) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[[InlineKeyboardButton(text="Да", callback_data=f"{prefix}:yes"), InlineKeyboardButton(text="Нет", callback_data=f"{prefix}:no")]]
    )
