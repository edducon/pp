from __future__ import annotations

import datetime as dt
import logging

from aiogram import Bot
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.cron import CronTrigger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker

from app.bot.keyboards.common import pause_keyboard
from app.enums import MigrationCardStatus
from app.models import MigrationCard, User
from app.services.localization import Translator

logger = logging.getLogger(__name__)


class Notifier:
    def __init__(self, scheduler: AsyncIOScheduler, session_factory: async_sessionmaker, bot: Bot, translator: Translator):
        self.scheduler = scheduler
        self.session_factory = session_factory
        self.bot = bot
        self.translator = translator

    def start(self) -> None:
        trigger = CronTrigger(hour=8, minute=0)
        self.scheduler.add_job(self._dispatch_notifications, trigger=trigger, id="migration_card_notifications", replace_existing=True)
        self.scheduler.start()
        logger.info("Scheduler started for periodic notifications")

    async def _dispatch_notifications(self) -> None:
        today = dt.date.today()
        async with self.session_factory() as session:
            stmt = select(MigrationCard).where(
                MigrationCard.status == MigrationCardStatus.ACTIVE,
                MigrationCard.paused_by_user.is_(False),
                MigrationCard.expires_at >= today,
                MigrationCard.expires_at <= today + dt.timedelta(days=30),
            )
            cards = list((await session.scalars(stmt)).all())
            for card in cards:
                days_left = (card.expires_at - today).days
                should_notify = card.last_notified_on != today and (days_left == 30 or days_left % 7 == 2 or days_left <= 2)
                if not should_notify:
                    continue
                user = await session.get(User, card.user_id)
                if not user:
                    continue
                await self._notify_user(user.telegram_id, user.language, card.expires_at, card.id)
                card.last_notified_on = today
                logger.info("Notified user %s about migration card %s", user.telegram_id, card.id)
            await session.commit()

    async def _notify_user(self, chat_id: int, locale: str, expires_at: dt.date, card_id: int) -> None:
        text = self.translator.t("notifications.expiring", locale, date=expires_at.strftime("%d.%m.%Y"))
        await self.bot.send_message(chat_id, text, reply_markup=pause_keyboard(self.translator.t("notifications.pause_button", locale), card_id))
