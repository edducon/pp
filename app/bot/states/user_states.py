from aiogram.fsm.state import State, StatesGroup


class RegistrationStates(StatesGroup):
    choose_language = State()
    enter_citizenship = State()
    confirm_citizenship = State()
    enter_phone = State()
    choose_notification_window = State()
    ask_temp_registration = State()
    ask_temp_registration_date = State()
    ask_living_place = State()


class DocumentStates(StatesGroup):
    select_document = State()
    set_expiry_date = State()
    set_entry_date = State()
    confirm_extension = State()
    new_expiry_after_extension = State()
    toggle_notifications = State()


class AdminStates(StatesGroup):
    broadcast_text = State()
    broadcast_confirm = State()
    add_admin = State()
