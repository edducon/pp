from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict


class Translator:
    def __init__(self, locales_path: Path):
        self.locales_path = locales_path
        self.translations: Dict[str, Dict[str, str]] = {}

    def load(self) -> None:
        for locale_file in self.locales_path.glob("*.json"):
            locale = locale_file.stem
            with locale_file.open("r", encoding="utf-8") as fp:
                self.translations[locale] = json.load(fp)

    def t(self, key: str, locale: str = "ru", **kwargs: Any) -> str:
        catalog = self.translations.get(locale) or self.translations.get("ru", {})
        template = catalog.get(key, key)
        for placeholder, value in kwargs.items():
            template = template.replace(f"{{{placeholder}}}", str(value))
        return template
