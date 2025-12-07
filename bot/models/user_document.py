from __future__ import annotations

from datetime import datetime, date

from sqlalchemy import Boolean, Column, Date, DateTime, ForeignKey, Integer
from sqlalchemy.orm import relationship

from .base import Base


class UserDocument(Base):
    __tablename__ = "user_documents"

    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    document_type_id = Column(Integer, ForeignKey("document_types.id", ondelete="CASCADE"), nullable=False)
    expiry_date = Column(Date, nullable=True)
    submitted_for_extension = Column(Boolean, default=False, nullable=False)
    notifications_enabled = Column(Boolean, default=True, nullable=False)
    last_notification_at = Column(DateTime, nullable=True)

    user = relationship("User", back_populates="documents")
    document_type = relationship("DocumentType", back_populates="user_documents")
    history = relationship("UserDocumentHistory", back_populates="document", cascade="all, delete-orphan")

    def __repr__(self) -> str:
        return f"UserDocument(id={self.id}, type={self.document_type_id})"


class UserDocumentHistory(Base):
    __tablename__ = "user_document_history"

    id = Column(Integer, primary_key=True)
    document_id = Column(Integer, ForeignKey("user_documents.id", ondelete="CASCADE"), nullable=False)
    old_expiry_date = Column(Date, nullable=True)
    new_expiry_date = Column(Date, nullable=True)
    changed_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    document = relationship("UserDocument", back_populates="history")
