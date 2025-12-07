import os
import asyncio
from logging.config import fileConfig

from sqlalchemy import pool
from sqlalchemy.ext.asyncio import async_engine_from_config

from alembic import context
from app.models.base import Base
from app.models import user, documents  # noqa: F401

config = context.config

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# --- Берём URL из переменной окружения и прописываем в конфиг ---
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise RuntimeError("DATABASE_URL env var is not set")

# Это заменит ${DATABASE_URL} в alembic.ini на реальную строку
config.set_main_option("sqlalchemy.url", DATABASE_URL)
# ----------------------------------------------------------------

target_metadata = Base.metadata


def run_migrations_offline() -> None:
    """Запуск миграций в оффлайновом режиме (генерится SQL)."""
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )

    with context.begin_transaction():
        context.run_migrations()


def do_run_migrations(connection):
    context.configure(connection=connection, target_metadata=target_metadata)

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """Запуск миграций в онлайне (на живой БД)."""
    connectable = async_engine_from_config(
        config.get_section(config.config_ini_section),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    async def run_migrations() -> None:
        async with connectable.connect() as connection:
            await connection.run_sync(do_run_migrations)

    asyncio.run(run_migrations())


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
