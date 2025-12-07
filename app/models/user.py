from __future__ import annotations

from datetime import datetime, time
from typing import List, Optional

from sqlalchemy import Boolean, DateTime, Integer, String, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class User(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    telegram_id: Mapped[int] = mapped_column(Integer, unique=True, index=True)
    username: Mapped[Optional[str]] = mapped_column(String(255))
    language: Mapped[str] = mapped_column(String(5), default="ru")
    citizenship_code: Mapped[Optional[str]] = mapped_column(String(10))
    citizenship_name: Mapped[Optional[str]] = mapped_column(String(255))
    phone: Mapped[Optional[str]] = mapped_column(String(32))
    notification_window_start: Mapped[Optional[time]] = mapped_column(Time(timezone=False))
    notification_window_end: Mapped[Optional[time]] = mapped_column(Time(timezone=False))
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    documents: Mapped[List["UserDocument"]] = relationship(back_populates="user", cascade="all, delete-orphan")

    def __repr__(self) -> str:
        return f"User(id={self.id}, telegram_id={self.telegram_id})"
