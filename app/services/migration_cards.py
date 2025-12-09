from __future__ import annotations

import datetime as dt
from typing import Optional

from sqlalchemy import and_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.enums import MigrationCardStatus, MigrationEvent
from app.models import MigrationCard, MigrationCardHistory


class MigrationCardService:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, user_id: int, expires_at: dt.date) -> MigrationCard:
        card = MigrationCard(user_id=user_id, expires_at=expires_at, status=MigrationCardStatus.ACTIVE)
        self.session.add(card)
        await self.session.flush()
        await self._add_history(card, MigrationEvent.CREATED, expires_at)
        return card

    async def update_expiry(self, card: MigrationCard, new_date: dt.date, event: MigrationEvent) -> MigrationCard:
        previous = card.expires_at
        card.expires_at = new_date
        card.status = MigrationCardStatus.ACTIVE
        card.paused_by_user = False
        card.last_notified_on = None
        card.needs_travel_confirmation = True
        await self._add_history(card, event, new_date, previous_expires_at=previous)
        return card

    async def pause_by_user(self, card: MigrationCard, note: str) -> None:
        card.paused_by_user = True
        card.status = MigrationCardStatus.SUBMITTED_FOR_EXTENSION
        await self._add_history(card, MigrationEvent.USER_PAUSED, card.expires_at, note=note)

    async def mark_extended_by_admin(self, card: MigrationCard, note: str | None = None) -> None:
        card.status = MigrationCardStatus.EXTENDED
        card.paused_by_user = False
        card.needs_travel_confirmation = True
        await self._add_history(card, MigrationEvent.ADMIN_MARKED_EXTENDED, card.expires_at, note=note)

    async def travel_confirmation(self, card: MigrationCard, in_russia: bool) -> None:
        await self._add_history(card, MigrationEvent.TRAVEL_CONFIRMED, card.expires_at, in_russia=in_russia)
        card.needs_travel_confirmation = False

    async def find_due_for_notifications(self, today: dt.date):
        stmt = select(MigrationCard).where(
            and_(
                MigrationCard.status == MigrationCardStatus.ACTIVE,
                MigrationCard.paused_by_user.is_(False),
                MigrationCard.expires_at >= today,
                MigrationCard.expires_at <= today + dt.timedelta(days=30),
            )
        )
        return list((await self.session.scalars(stmt)).all())

    async def _add_history(
        self,
        card: MigrationCard,
        event: MigrationEvent,
        expires_at: Optional[dt.date],
        *,
        previous_expires_at: Optional[dt.date] = None,
        note: Optional[str] = None,
        in_russia: Optional[bool] = None,
    ) -> None:
        self.session.add(
            MigrationCardHistory(
                migration_card_id=card.id,
                user_id=card.user_id,
                event=event,
                previous_expires_at=previous_expires_at,
                expires_at=expires_at,
                in_russia=in_russia,
                note=note,
            )
        )
