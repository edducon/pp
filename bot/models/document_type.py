from __future__ import annotations

from sqlalchemy import Column, Integer, JSON, String
from sqlalchemy.orm import relationship

from .base import Base


class DocumentType(Base):
    __tablename__ = "document_types"

    id = Column(Integer, primary_key=True)
    code = Column(String(50), unique=True, nullable=False)
    name_ru = Column(String(255), nullable=False)
    name_en = Column(String(255), nullable=False)
    rules = Column(JSON, nullable=True)

    user_documents = relationship("UserDocument", back_populates="document_type")

    def __repr__(self) -> str:
        return f"DocumentType(code={self.code})"
