from __future__ import annotations

from aiogram.types import InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup
from aiogram.utils.keyboard import InlineKeyboardBuilder

from bot.services.citizenship import Country


def language_keyboard(t) -> InlineKeyboardMarkup:
    builder = InlineKeyboardBuilder()
    builder.button(text=t("language.ru"), callback_data="lang:ru")
    builder.button(text=t("language.en"), callback_data="lang:en")
    builder.adjust(2)
    return builder.as_markup()


def countries_keyboard(countries: list[Country]) -> InlineKeyboardMarkup:
    builder = InlineKeyboardBuilder()
    for country in countries:
        builder.button(text=country.display_name, callback_data=f"cit:{country.code}")
    builder.adjust(2)
    return builder.as_markup()


def citizenship_confirm_keyboard(code: str, t) -> InlineKeyboardMarkup:
    builder = InlineKeyboardBuilder()
    builder.button(text=t("actions.yes"), callback_data=f"cit-confirm:{code}")
    builder.button(text=t("actions.no"), callback_data="cit-retry")
    builder.adjust(2)
    return builder.as_markup()


def notification_window_keyboard(t) -> InlineKeyboardMarkup:
    builder = InlineKeyboardBuilder()
    for window in ["09:00-18:00", "09:00-22:00", "12:00-22:00"]:
        builder.button(text=t("window.option", window=window), callback_data=f"win:{window}")
    builder.adjust(1)
    return builder.as_markup()


def cancel_keyboard(t) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[[KeyboardButton(text=t("actions.cancel"))]], resize_keyboard=True, one_time_keyboard=True
    )


def contact_keyboard(t) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text=t("onboarding.share_phone"), request_contact=True)],
            [KeyboardButton(text=t("actions.cancel"))],
        ],
        resize_keyboard=True,
        one_time_keyboard=True,
    )


def main_menu_keyboard(t) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [
                KeyboardButton(text=t("menu.documents")),
                KeyboardButton(text=t("menu.settings")),
            ],
            [KeyboardButton(text=t("menu.help"))],
        ],
        resize_keyboard=True,
    )
