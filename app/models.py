from __future__ import annotations

import datetime as dt
from typing import List, Optional

from sqlalchemy import BigInteger, Boolean, Date, DateTime, Enum, ForeignKey, Integer, String, JSON, UniqueConstraint, func, Index
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

from app.enums import MigrationCardStatus, MigrationEvent


class Base(DeclarativeBase):
    pass


class Country(Base):
    __tablename__ = "countries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    code3: Mapped[str] = mapped_column(String(3), unique=True, nullable=False)
    name_ru: Mapped[str] = mapped_column(String(128), nullable=False)
    name_en: Mapped[str] = mapped_column(String(128), nullable=False)
    aliases: Mapped[List[str]] = mapped_column(JSON, default=list)

    users: Mapped[List["User"]] = relationship(back_populates="citizenship")


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    telegram_id: Mapped[int] = mapped_column(BigInteger, unique=True, nullable=False, index=True)
    language: Mapped[str] = mapped_column(String(8), default="ru", nullable=False)
    first_name: Mapped[str] = mapped_column(String(128), nullable=False)
    last_name: Mapped[str] = mapped_column(String(128), nullable=False)
    patronymic: Mapped[Optional[str]] = mapped_column(String(128))
    citizenship_id: Mapped[Optional[int]] = mapped_column(ForeignKey("countries.id"))
    phone_from_telegram: Mapped[Optional[str]] = mapped_column(String(32))
    manual_phone: Mapped[Optional[str]] = mapped_column(String(32))
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    citizenship: Mapped[Optional[Country]] = relationship(back_populates="users")
    migration_cards: Mapped[List["MigrationCard"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    admin: Mapped[Optional["Admin"]] = relationship(back_populates="user", uselist=False)


class Admin(Base):
    __tablename__ = "admins"

    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), primary_key=True)
    granted_by: Mapped[int] = mapped_column(BigInteger, nullable=False)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    user: Mapped[User] = relationship(back_populates="admin")


class MigrationCard(Base):
    __tablename__ = "migration_cards"
    __table_args__ = (
        UniqueConstraint("user_id", name="unique_active_card"),
        Index("idx_migration_cards_expires", "expires_at"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    expires_at: Mapped[dt.date] = mapped_column(Date, nullable=False)
    status: Mapped[MigrationCardStatus] = mapped_column(Enum(MigrationCardStatus), default=MigrationCardStatus.ACTIVE)
    paused_by_user: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    last_notified_on: Mapped[Optional[dt.date]] = mapped_column(Date)
    needs_travel_confirmation: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    user: Mapped[User] = relationship(back_populates="migration_cards")
    history: Mapped[List["MigrationCardHistory"]] = relationship(back_populates="migration_card", cascade="all, delete-orphan")


class MigrationCardHistory(Base):
    __tablename__ = "migration_card_history"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    migration_card_id: Mapped[int] = mapped_column(ForeignKey("migration_cards.id"))
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"))
    event: Mapped[MigrationEvent] = mapped_column(Enum(MigrationEvent))
    previous_expires_at: Mapped[Optional[dt.date]] = mapped_column(Date)
    expires_at: Mapped[Optional[dt.date]] = mapped_column(Date)
    in_russia: Mapped[Optional[bool]] = mapped_column(Boolean)
    note: Mapped[Optional[str]] = mapped_column(String(255))
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    migration_card: Mapped[MigrationCard] = relationship(back_populates="history")
