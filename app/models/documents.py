from datetime import date, datetime
from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .base import Base, now_utc


class DocumentType(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    code: Mapped[str] = mapped_column(String(64), unique=True, nullable=False, index=True)
    name_ru: Mapped[str] = mapped_column(String(255), nullable=False)
    name_en: Mapped[str] = mapped_column(String(255), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    documents: Mapped[list["UserDocument"]] = relationship(back_populates="document_type", cascade="all, delete")


class UserDocument(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("user.id", ondelete="CASCADE"), nullable=False, index=True)
    document_type_id: Mapped[int] = mapped_column(ForeignKey("documenttype.id", ondelete="CASCADE"), nullable=False)
    current_expiry_date: Mapped[date | None] = mapped_column(Date)
    entry_date: Mapped[date | None] = mapped_column(Date)
    submitted_for_extension: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    extension_expiry_date: Mapped[date | None] = mapped_column(Date)
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    last_notification_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    final_reminder_sent: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now_utc, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now_utc, onupdate=now_utc, nullable=False)

    user: Mapped["User"] = relationship(back_populates="documents")
    document_type: Mapped[DocumentType] = relationship(back_populates="documents")
    history: Mapped[list["UserDocumentHistory"]] = relationship(
        back_populates="user_document", cascade="all, delete-orphan"
    )


class UserDocumentHistory(Base):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_document_id: Mapped[int] = mapped_column(
        ForeignKey("userdocument.id", ondelete="CASCADE"), nullable=False, index=True
    )
    old_expiry_date: Mapped[date | None] = mapped_column(Date)
    new_expiry_date: Mapped[date | None] = mapped_column(Date)
    changed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now_utc, nullable=False)
    changed_by: Mapped[str] = mapped_column(String(64), nullable=False, default="user")
    comment: Mapped[str | None] = mapped_column(String(255))

    user_document: Mapped[UserDocument] = relationship(back_populates="history")


from .user import User  # noqa: E402  # circular imports
