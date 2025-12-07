from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Dict


@dataclass
class Translator:
    language: str
    messages: Dict[str, Any]

    def t(self, key: str, **kwargs: Any) -> str:
        parts = key.split(".")
        node: Any = self.messages
        for part in parts:
            node = node.get(part, {})
        if not isinstance(node, str):
            return key
        return node.format(**kwargs)


class TranslationLoader:
    def __init__(self, locales_path: Path) -> None:
        self.locales_path = locales_path
        self.locales: Dict[str, Dict[str, Any]] = {}

    def load(self) -> None:
        for path in self.locales_path.glob("*.json"):
            with path.open("r", encoding="utf-8") as fh:
                self.locales[path.stem] = json.load(fh)

    def get_translator(self, language: str) -> Translator:
        lang = language if language in self.locales else "ru"
        return Translator(lang, self.locales.get(lang, {}))


__all__ = ["Translator", "TranslationLoader"]
