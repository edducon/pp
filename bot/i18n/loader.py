from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict

from aiogram.types import CallbackQuery, Message


SUPPORTED_LANGUAGES = ("ru", "en")
DEFAULT_LANGUAGE = "ru"


@dataclass
class Translator:
    language: str
    catalog: Dict[str, str]

    def t(self, key: str, **kwargs: Any) -> str:
        template = self.catalog.get(key, key)
        return template.format(**kwargs)


class TranslationLoader:
    def __init__(self, locales_dir: Path) -> None:
        self.locales_dir = locales_dir
        self._catalogs: dict[str, dict[str, str]] = {}

    def load(self) -> None:
        for lang in SUPPORTED_LANGUAGES:
            path = self.locales_dir / f"{lang}.json"
            with path.open("r", encoding="utf-8") as f:
                self._catalogs[lang] = json.load(f)

    def get_translator(self, lang: str | None) -> Translator:
        language = lang if lang in self._catalogs else DEFAULT_LANGUAGE
        return Translator(language=language, catalog=self._catalogs.get(language, {}))

    def get_user_language(self, obj: Message | CallbackQuery) -> str:
        lang_code = None
        if isinstance(obj, Message):
            lang_code = obj.from_user.language_code
        else:
            lang_code = obj.from_user.language_code
        if not lang_code:
            return DEFAULT_LANGUAGE
        base = lang_code.split("-")[0]
        return base if base in SUPPORTED_LANGUAGES else DEFAULT_LANGUAGE


__all__ = ["TranslationLoader", "Translator", "SUPPORTED_LANGUAGES", "DEFAULT_LANGUAGE"]
