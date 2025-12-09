from __future__ import annotations

from aiogram.fsm.state import State, StatesGroup


class RegistrationState(StatesGroup):
    choosing_language = State()
    collecting_first_name = State()
    collecting_last_name = State()
    collecting_patronymic = State()
    collecting_citizenship = State()
    sharing_phone = State()
    manual_phone = State()
    collecting_expiry = State()


class AdminBroadcastState(StatesGroup):
    waiting_for_dates = State()
    waiting_for_message = State()


class TravelCheckState(StatesGroup):
    awaiting_confirmation = State()
