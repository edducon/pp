from __future__ import annotations

from sqlalchemy import Boolean, Column, ForeignKey, Integer, String, Time
from sqlalchemy.orm import relationship

from .base import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True)
    telegram_id = Column(Integer, unique=True, nullable=False, index=True)
    language = Column(String(5), nullable=False, default="ru")
    citizenship_code = Column(String(10), nullable=False)
    phone = Column(String(20), nullable=True)
    notification_time_start = Column(Time, nullable=False)
    notification_time_end = Column(Time, nullable=False)
    is_admin = Column(Boolean, default=False, nullable=False)

    documents = relationship("UserDocument", back_populates="user", cascade="all, delete-orphan")

    def __repr__(self) -> str:
        return f"User(id={self.id}, telegram_id={self.telegram_id})"
