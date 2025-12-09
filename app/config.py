from __future__ import annotations

from pydantic import BaseModel
from pydantic_settings import BaseSettings, SettingsConfigDict


class DatabaseSettings(BaseModel):
    # URL для SQLAlchemy async-движка
    url: str


class BotSettings(BaseSettings):
    # Имена полей → что ты реально используешь в коде (settings.token и т.д.)
    token: str
    superadmin_id: int
    timezone: str = "Europe/Moscow"
    database: DatabaseSettings

    # Настройки чтения из .env
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        env_prefix="BOT_",          # BOT_TOKEN, BOT_SUPERADMIN_ID, BOT_TIMEZONE, BOT_DATABASE__URL
        env_nested_delimiter="__",  # BOT_DATABASE__URL → database.url
        extra="ignore",             # лишние переменные в .env игнорируются
    )


def load_settings() -> BotSettings:
    return BotSettings()
