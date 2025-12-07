from __future__ import annotations
from datetime import datetime, time
from zoneinfo import ZoneInfo

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.documents import DocumentType, UserDocument, UserDocumentHistory
from app.models.user import User

DEFAULT_DOCUMENTS = [
    {"code": "MIGRATION_CARD", "name_ru": "Миграционная карта", "name_en": "Migration card"},
    {"code": "TEMP_REGISTRATION", "name_ru": "Временная регистрация", "name_en": "Temporary registration"},
    {"code": "MED_CERT_1", "name_ru": "Мед. сертификат 1", "name_en": "Medical certificate 1"},
    {"code": "MED_CERT_2", "name_ru": "Мед. сертификат 2", "name_en": "Medical certificate 2"},
    {"code": "MED_CERT_3", "name_ru": "Мед. сертификат 3", "name_en": "Medical certificate 3"},
    {"code": "VISA", "name_ru": "Виза", "name_en": "Visa"},
]


def parse_time_window(value: str) -> tuple[time, time]:
    start_str, end_str = value.split("-")
    start = datetime.strptime(start_str, "%H:%M").time()
    end = datetime.strptime(end_str, "%H:%M").time()
    return start, end


async def ensure_document_types(session: AsyncSession) -> None:
    existing = {row.code for row in (await session.execute(select(DocumentType))).scalars()}
    for doc in DEFAULT_DOCUMENTS:
        if doc["code"] not in existing:
            session.add(DocumentType(**doc))
    await session.commit()


async def ensure_user_documents(session: AsyncSession, user: User) -> list[UserDocument]:
    await ensure_document_types(session)
    result = await session.execute(select(UserDocument).where(UserDocument.user_id == user.id))
    existing_docs = {doc.document_type_id: doc for doc in result.scalars().all()}

    type_rows = (await session.execute(select(DocumentType))).scalars().all()
    created_docs: list[UserDocument] = []
    for doc_type in type_rows:
        if doc_type.id in existing_docs:
            continue
        doc = UserDocument(user_id=user.id, document_type_id=doc_type.id)
        session.add(doc)
        created_docs.append(doc)
    if created_docs:
        await session.commit()
    else:
        await session.flush()
    return list(existing_docs.values()) + created_docs


async def add_history_record(
    session: AsyncSession,
    user_document: UserDocument,
    old_date,
    new_date,
    changed_by: str = "user",
    comment: str | None = None,
) -> UserDocumentHistory:
    record = UserDocumentHistory(
        user_document_id=user_document.id,
        old_expiry_date=old_date,
        new_expiry_date=new_date,
        changed_by=changed_by,
        comment=comment,
    )
    session.add(record)
    await session.commit()
    return record


def within_window(now_dt: datetime, start: time, end: time, tz: str) -> bool:
    local = now_dt.astimezone(ZoneInfo(tz))
    local_time = local.time()
    if start <= end:
        return start <= local_time <= end
    return local_time >= start or local_time <= end
