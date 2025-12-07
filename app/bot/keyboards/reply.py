from aiogram.types import KeyboardButton, ReplyKeyboardMarkup, ReplyKeyboardRemove


def main_menu_keyboard(t) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[
            [KeyboardButton(text=t("menu.documents"))],
            [KeyboardButton(text=t("menu.settings")), KeyboardButton(text=t("menu.help"))],
        ],
        resize_keyboard=True,
    )


def contact_keyboard(t) -> ReplyKeyboardMarkup:
    return ReplyKeyboardMarkup(
        keyboard=[[KeyboardButton(text=t("start.share_contact"), request_contact=True)],
                  [KeyboardButton(text=t("buttons.skip"))]],
        resize_keyboard=True,
        one_time_keyboard=True,
    )


def remove_keyboard() -> ReplyKeyboardRemove:
    return ReplyKeyboardRemove()
