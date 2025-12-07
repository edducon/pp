from __future__ import annotations

from datetime import date, datetime
from typing import List, Optional

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class DocumentType(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    code: Mapped[str] = mapped_column(String(50), unique=True, index=True)
    name_ru: Mapped[str] = mapped_column(String(255))
    name_en: Mapped[str] = mapped_column(String(255))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    documents: Mapped[List["UserDocument"]] = relationship(back_populates="document_type", cascade="all, delete-orphan")


class UserDocument(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("user.id", ondelete="CASCADE"), index=True)
    document_type_id: Mapped[int] = mapped_column(ForeignKey("documenttype.id", ondelete="CASCADE"))
    current_expiry_date: Mapped[Optional[date]] = mapped_column(Date())
    entry_date: Mapped[Optional[date]] = mapped_column(Date())
    submitted_for_extension: Mapped[bool] = mapped_column(Boolean, default=False)
    extension_expiry_date: Mapped[Optional[date]] = mapped_column(Date())
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    last_notification_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True))
    final_reminder_sent: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="documents")
    document_type: Mapped[DocumentType] = relationship(back_populates="documents")
    history_records: Mapped[List["UserDocumentHistory"]] = relationship(back_populates="document", cascade="all, delete-orphan")


class UserDocumentHistory(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_document_id: Mapped[int] = mapped_column(ForeignKey("userdocument.id", ondelete="CASCADE"))
    old_expiry_date: Mapped[Optional[date]] = mapped_column(Date())
    new_expiry_date: Mapped[Optional[date]] = mapped_column(Date())
    changed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=datetime.utcnow)
    changed_by: Mapped[str] = mapped_column(String(32), default="user")
    comment: Mapped[Optional[str]] = mapped_column(String(255))

    document: Mapped[UserDocument] = relationship(back_populates="history_records")
