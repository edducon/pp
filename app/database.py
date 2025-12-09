from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from pathlib import Path
from typing import AsyncIterator

from alembic import command
from alembic.config import Config
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine

from app.config import BotSettings


class Database:
    def __init__(self, settings: BotSettings):
        self.settings = settings
        self.engine: AsyncEngine = create_async_engine(settings.database.url, echo=False, future=True)
        self.session_factory: async_sessionmaker[AsyncSession] = async_sessionmaker(self.engine, expire_on_commit=False)
        self._alembic_config = self._load_alembic_config()

    def _load_alembic_config(self) -> Config:
        cfg = Config(str(Path(__file__).resolve().parent.parent / "alembic.ini"))
        cfg.set_main_option("script_location", "migrations")
        cfg.set_main_option("sqlalchemy.url", self.settings.database.url)
        return cfg

    async def run_migrations(self) -> None:
        await asyncio.to_thread(command.upgrade, self._alembic_config, "head")

    @asynccontextmanager
    async def session(self) -> AsyncIterator[AsyncSession]:
        session: AsyncSession = self.session_factory()
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
