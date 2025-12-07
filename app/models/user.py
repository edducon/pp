from datetime import datetime, time
from sqlalchemy import BigInteger, Boolean, DateTime, Integer, String, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .base import Base, now_utc


class User(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    telegram_id: Mapped[int] = mapped_column(BigInteger, unique=True, index=True, nullable=False)
    username: Mapped[str | None] = mapped_column(String(255))
    language: Mapped[str] = mapped_column(String(8), default="ru", nullable=False)
    citizenship_code: Mapped[str | None] = mapped_column(String(8))
    citizenship_name: Mapped[str | None] = mapped_column(String(255))
    phone: Mapped[str | None] = mapped_column(String(32))
    notification_window_start: Mapped[time | None] = mapped_column(Time(timezone=False))
    notification_window_end: Mapped[time | None] = mapped_column(Time(timezone=False))
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now_utc, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=now_utc, onupdate=now_utc, nullable=False
    )

    documents: Mapped[list["UserDocument"]] = relationship(back_populates="user", cascade="all, delete-orphan")


from .documents import UserDocument  # noqa: E402  # to avoid circular import
