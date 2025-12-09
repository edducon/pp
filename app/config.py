from __future__ import annotations

from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class DatabaseSettings(BaseModel):
    url: str = Field(..., description="SQLAlchemy async database URL")


class BotSettings(BaseSettings):
    # Настройки: читаем из .env и из окружения, с префиксом BOT_
    model_config = SettingsConfigDict(
        env_file=".env",
        env_nested_delimiter="__",
        env_prefix="BOT_",
        extra="ignore",
    )

    # Эти поля будут читаться из:
    # BOT_TOKEN, BOT_SUPERADMIN_ID, BOT_TIMEZONE
    token: str = Field(..., description="Telegram bot token")
    superadmin_id: int = Field(
        ..., description="Telegram user ID of the super administrator"
    )
    timezone: str = Field(
        "Europe/Moscow",
        description="Default timezone for schedulers",
    )

    # Это поле — из BOT_DATABASE__URL (nested_delimiter="__")
    database: DatabaseSettings = DatabaseSettings(
        # дефолт, если переменной нет (локальный запуск)
        url="postgresql+asyncpg://admin:qwerty12345@localhost:5432/botdb1"
    )


def load_settings() -> BotSettings:
    return BotSettings()
