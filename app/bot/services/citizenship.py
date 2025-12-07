from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional, Tuple

from rapidfuzz import fuzz


@dataclass
class Citizenship:
    code: str
    name_en: str
    name_ru: str
    synonyms: List[str]


def _normalize_value(value: str) -> str:
    return value.strip().lower()


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
    normalized_query = _normalize_value(query)
    synonyms = [_normalize_value(syn) for syn in candidate.synonyms]

    # Short queries must match by prefix or substring to avoid noisy results like "az" vs "kaz".
    if len(normalized_query) <= 3:
        if any(syn.startswith(normalized_query) for syn in synonyms):
            return 100
        if any(normalized_query in syn for syn in synonyms):
            return 85
        return 0

    base_score = max(fuzz.partial_ratio(normalized_query, syn) for syn in synonyms)
    if any(syn.startswith(normalized_query) for syn in synonyms):
        base_score = max(base_score, 100)
    elif any(normalized_query in syn for syn in synonyms):
        base_score = max(base_score, 90)
    return base_score


def find_matches(query: str) -> List[Tuple[Citizenship, int]]:
    normalized = _normalize_value(query)
    if not normalized:
        return []
    scores = [(_score(normalized, c), c) for c in CITIZENSHIPS]
    scores = [(score, c) for score, c in scores if score >= 70]
    scores.sort(key=lambda item: item[0], reverse=True)
    return [(c, score) for score, c in scores]


def best_match(query: str) -> Optional[Tuple[Citizenship, int]]:
    matches = find_matches(query)
    return matches[0] if matches else None
