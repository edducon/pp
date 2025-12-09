from __future__ import annotations

import enum


class MigrationCardStatus(str, enum.Enum):
    ACTIVE = "active"
    SUBMITTED_FOR_EXTENSION = "submitted_for_extension"
    EXTENDED = "extended"
    PAUSED_BY_ADMIN = "paused_by_admin"


class MigrationEvent(str, enum.Enum):
    CREATED = "created"
    EXTENDED = "extended"
    USER_PAUSED = "user_paused"
    ADMIN_MARKED_EXTENDED = "admin_marked_extended"
    TRAVEL_CHECK = "travel_check"
    TRAVEL_CONFIRMED = "travel_confirmed"
