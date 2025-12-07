from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton


def language_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[[InlineKeyboardButton(text="Русский", callback_data="lang:ru")],
                         [InlineKeyboardButton(text="English", callback_data="lang:en")]]
    )


def citizenship_options(options: list[tuple[str, str]]) -> InlineKeyboardMarkup:
    buttons = [
        [InlineKeyboardButton(text=name, callback_data=f"cit:{code}")]
        for code, name in options
    ]
    buttons.append([InlineKeyboardButton(text="⬅️", callback_data="cit:none")])
    return InlineKeyboardMarkup(inline_keyboard=buttons)


def confirm_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="✅", callback_data="confirm:yes"),
             InlineKeyboardButton(text="❌", callback_data="confirm:no")],
        ]
    )


def notification_windows_keyboard(windows: list[tuple[str, str]]) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text=label, callback_data=f"notify:{code}")] for code, label in windows
        ]
    )


def yes_no_keyboard(yes_cb: str = "yes", no_cb: str = "no") -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[[InlineKeyboardButton(text="✅", callback_data=yes_cb), InlineKeyboardButton(text="❌", callback_data=no_cb)]]
    )


def document_actions_keyboard(doc_id: int) -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(
        inline_keyboard=[
            [InlineKeyboardButton(text="✏️", callback_data=f"doc:expiry:{doc_id}"),
             InlineKeyboardButton(text="📥", callback_data=f"doc:extend:{doc_id}")],
            [InlineKeyboardButton(text="🔕", callback_data=f"doc:toggle:{doc_id}")],
        ]
    )
