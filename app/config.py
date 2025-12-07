from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Optional

from pydantic import BaseSettings, Field


class Settings(BaseSettings):
    bot_token: str = Field(..., env="BOT_TOKEN")
    database_url: str = Field(..., env="DATABASE_URL")
    superadmin_id: int = Field(..., env="SUPERADMIN_ID")
    timezone: str = Field("Europe/Moscow", env="TIMEZONE")

    log_level: str = Field("INFO", env="LOG_LEVEL")
    log_file: Path = Field(Path("logs/bot.log"), env="LOG_FILE")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
