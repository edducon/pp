from __future__ import annotations

import re
from dataclasses import dataclass
from typing import List

from rapidfuzz import fuzz


@dataclass
class Country:
    code: str
    names: list[str]
    visa_required: bool
    eaeu: bool = False


COUNTRIES: list[Country] = [
    Country(code="ARM", names=["armenia", "армен", "arm"], visa_required=False, eaeu=True),
    Country(code="KAZ", names=["kazakhstan", "каз", "kaz"], visa_required=False, eaeu=True),
    Country(code="KGZ", names=["kyrgyzstan", "кырг", "kg", "kgz"], visa_required=False, eaeu=True),
    Country(code="BLR", names=["belarus", "бел", "blr"], visa_required=False, eaeu=True),
    Country(code="RUS", names=["russia", "рос", "rus"], visa_required=False, eaeu=True),
    Country(code="UZB", names=["uzbekistan", "узб", "uzb"], visa_required=False, eaeu=False),
    Country(code="TJK", names=["tajikistan", "тадж", "tjk"], visa_required=True, eaeu=False),
    Country(code="UKR", names=["ukraine", "украин", "ukr"], visa_required=False, eaeu=False),
    Country(code="CHN", names=["china", "кит", "chn"], visa_required=True, eaeu=False),
    Country(code="IND", names=["india", "инд", "ind"], visa_required=True, eaeu=False),
]


def normalize(value: str) -> str:
    return re.sub(r"[^a-zа-я0-9]", "", value.lower())


def match_countries(query: str, limit: int = 5) -> List[Country]:
    norm = normalize(query)
    scored: list[tuple[int, Country]] = []
    for country in COUNTRIES:
        best = max(fuzz.partial_ratio(norm, normalize(name)) for name in country.names)
        scored.append((best, country))
    scored.sort(key=lambda item: item[0], reverse=True)
    return [country for score, country in scored if score >= 50][:limit]


def is_visa_required(code: str) -> bool:
    for country in COUNTRIES:
        if country.code == code:
            return country.visa_required and not country.eaeu
    return False


def is_eaeu(code: str) -> bool:
    for country in COUNTRIES:
        if country.code == code:
            return country.eaeu
    return False
