from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from rapidfuzz import fuzz


@dataclass
class Citizenship:
    code: str
    name_en: str
    name_ru: str
    synonyms: List[str]


CITIZENSHIPS: List[Citizenship] = [
    Citizenship("KAZ", "Kazakhstan", "Казахстан", ["kaz", "каз", "kazakhstan", "казахстан"]),
    Citizenship("KGZ", "Kyrgyzstan", "Кыргызстан", ["kgz", "kg", "кырг", "киргизия", "kyrgyzstan"]),
    Citizenship("UZB", "Uzbekistan", "Узбекистан", ["uzb", "uz", "uzbekistan", "узбекистан"]),
    Citizenship("ARM", "Armenia", "Армения", ["arm", "armenia", "армения"]),
    Citizenship("TJK", "Tajikistan", "Таджикистан", ["tjk", "tajikistan", "таджикистан"]),
    Citizenship("AZE", "Azerbaijan", "Азербайджан", ["aze", "azerbaijan", "азербайджан"]),
    Citizenship("BLR", "Belarus", "Беларусь", ["blr", "belarus", "беларусь"]),
    Citizenship("GEO", "Georgia", "Грузия", ["geo", "georgia", "грузия"]),
    Citizenship("MDA", "Moldova", "Молдова", ["mda", "moldova", "молдова"]),
]


def _score(query: str, candidate: Citizenship) -> int:
    return max(fuzz.partial_ratio(query, syn) for syn in candidate.synonyms)


def find_matches(query: str) -> List[Tuple[Citizenship, int]]:
    normalized = query.strip().lower()
    if not normalized:
        return []
    scores = [(_score(normalized, c), c) for c in CITIZENSHIPS]
    scores = [(score, c) for score, c in scores if score >= 60]
    scores.sort(key=lambda item: item[0], reverse=True)
    return [(c, score) for score, c in scores]


def best_match(query: str) -> Optional[Tuple[Citizenship, int]]:
    matches = find_matches(query)
    return matches[0] if matches else None
