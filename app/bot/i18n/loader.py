import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from datetime import date


@dataclass
class Translator:
    translations: dict[str, dict[str, Any]]
    fallback_language: str = "ru"

    def gettext(self, language: str, key: str, **kwargs: Any) -> str:
        lang_data = self.translations.get(language) or self.translations.get(self.fallback_language, {})
        value = self._resolve_key(lang_data, key) or key
        try:
            return value.format(**kwargs)
        except KeyError:
            return value

    def _resolve_key(self, lang_data: dict[str, Any], key: str) -> str | None:
        parts = key.split(".")
        node: Any = lang_data
        for part in parts:
            if not isinstance(node, dict) or part not in node:
                return None
            node = node[part]
        if isinstance(node, str):
            return node
        return None

    def format_date(self, value: date | None, language: str) -> str:
        if value is None:
            return "-"
        if language.lower().startswith("ru"):
            return value.strftime("%d.%m.%Y")
        return value.strftime("%Y-%m-%d")


class TranslationLoader:
    def __init__(self, locales_path: Path):
        self.locales_path = locales_path

    def load(self) -> Translator:
        translations: dict[str, dict[str, Any]] = {}
        for file in self.locales_path.glob("*.json"):
            with file.open("r", encoding="utf-8") as f:
                translations[file.stem] = json.load(f)
        return Translator(translations=translations)
