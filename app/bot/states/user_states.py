from aiogram.fsm.state import State, StatesGroup


class Onboarding(StatesGroup):
    choosing_language = State()
    entering_citizenship = State()
    confirming_citizenship = State()
    entering_phone = State()
    choosing_window = State()


class DocumentStates(StatesGroup):
    entering_expiry = State()
    entering_entry_date = State()


class BroadcastStates(StatesGroup):
    waiting_text = State()
    waiting_filters = State()
    confirming = State()
