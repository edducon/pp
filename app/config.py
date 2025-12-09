from __future__ import annotations

from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class DatabaseSettings(BaseModel):
    url: str = Field(..., description="SQLAlchemy async database URL")


class BotSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_nested_delimiter="__", env_prefix="BOT_", extra="ignore")

    token: str = Field(..., alias="TOKEN", description="Telegram bot token")
    superadmin_id: int = Field(..., alias="SUPERADMIN_ID", description="Telegram user ID of the super administrator")
    timezone: str = Field("Europe/Moscow", alias="TIMEZONE", description="Default timezone for schedulers")
    database: DatabaseSettings = DatabaseSettings(url="sqlite+aiosqlite:///./bot.db")


def load_settings() -> BotSettings:
    return BotSettings()
