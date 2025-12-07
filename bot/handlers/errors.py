from __future__ import annotations

import logging

from aiogram import Router
from aiogram.types import ErrorEvent

router = Router()
logger = logging.getLogger(__name__)


@router.errors()
async def errors_handler(event: ErrorEvent) -> None:
    logger.exception("Unhandled error: %s", event.exception)
