from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Iterable, Optional

from sqlalchemy import and_, select
from sqlalchemy.ext.asyncio import AsyncSession

from bot.models import DocumentType, User, UserDocument, UserDocumentHistory
from bot.services.citizenship import is_visa_required, is_eaeu

MANDATORY_DOCS = ["MIGRATION_CARD", "TEMP_REG", "MED_1", "MED_2", "MED_3"]
VISA_CODE = "VISA"


class DocumentService:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def ensure_document_types(self) -> None:
        existing = await self.session.execute(select(DocumentType))
        if existing.scalars().first():
            return
        docs = [
            DocumentType(code="MIGRATION_CARD", name_ru="Миграционная карта", name_en="Migration card"),
            DocumentType(code="TEMP_REG", name_ru="Временная регистрация", name_en="Temporary registration"),
            DocumentType(code="MED_1", name_ru="Медицинская справка 1", name_en="Medical certificate 1"),
            DocumentType(code="MED_2", name_ru="Медицинская справка 2", name_en="Medical certificate 2"),
            DocumentType(code="MED_3", name_ru="Медицинская справка 3", name_en="Medical certificate 3"),
            DocumentType(code=VISA_CODE, name_ru="Виза", name_en="Visa", rules={"reminder_days": 45}),
        ]
        self.session.add_all(docs)
        await self.session.flush()

    async def bootstrap_user_documents(self, user: User) -> None:
        doc_types = await self.session.execute(select(DocumentType))
        doc_types_map = {dt.code: dt for dt in doc_types.scalars().all()}
        for code in MANDATORY_DOCS:
            if code not in doc_types_map:
                continue
            await self._ensure_user_doc(user, doc_types_map[code])
        if is_visa_required(user.citizenship_code):
            await self._ensure_user_doc(user, doc_types_map[VISA_CODE])

    async def _ensure_user_doc(self, user: User, doc_type: DocumentType) -> None:
        exists = await self.session.execute(
            select(UserDocument).where(
                and_(UserDocument.user_id == user.id, UserDocument.document_type_id == doc_type.id)
            )
        )
        if not exists.scalars().first():
            doc = UserDocument(user=user, document_type=doc_type)
            self.session.add(doc)
            await self.session.flush()

    async def update_expiry(self, document: UserDocument, new_date: date) -> None:
        history = UserDocumentHistory(
            document=document, old_expiry_date=document.expiry_date, new_expiry_date=new_date
        )
        document.expiry_date = new_date
        document.submitted_for_extension = False
        self.session.add(history)
        await self.session.flush()

    async def mark_submitted(self, document: UserDocument) -> None:
        document.submitted_for_extension = True
        await self.session.flush()

    async def get_user_documents(self, user: User) -> Iterable[UserDocument]:
        result = await self.session.execute(
            select(UserDocument).join(DocumentType).where(UserDocument.user_id == user.id)
        )
        return result.scalars().all()

    async def next_reminders(self, now: datetime, tz) -> list[UserDocument]:
        result = await self.session.execute(select(UserDocument).join(DocumentType).where(UserDocument.notifications_enabled))
        docs: list[UserDocument] = []
        for doc in result.scalars().all():
            if not doc.expiry_date:
                continue
            if doc.submitted_for_extension:
                if not doc.last_notification_at:
                    docs.append(doc)
                continue
            days_left = (doc.expiry_date - now.date()).days
            if days_left == 30 or (days_left <= 30 and days_left % 7 == 0) or days_left <= 1:
                docs.append(doc)
        return docs
