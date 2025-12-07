from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache

from dotenv import load_dotenv


load_dotenv()


@dataclass
class Settings:
    bot_token: str
    database_url: str
    superadmin_id: int
    timezone: str = "Europe/Moscow"
    rate_limit_per_minute: int = 30


@lru_cache()
def get_settings() -> Settings:
    bot_token = os.getenv("BOT_TOKEN")
    db_url = os.getenv("DATABASE_URL")
    superadmin = os.getenv("SUPERADMIN_ID")

    if not bot_token or not db_url or not superadmin:
        missing = [name for name, value in [
            ("BOT_TOKEN", bot_token),
            ("DATABASE_URL", db_url),
            ("SUPERADMIN_ID", superadmin),
        ] if not value]
        raise RuntimeError(f"Missing environment variables: {', '.join(missing)}")

    return Settings(
        bot_token=bot_token,
        database_url=db_url,
        superadmin_id=int(superadmin),
    )


__all__ = ["Settings", "get_settings"]
