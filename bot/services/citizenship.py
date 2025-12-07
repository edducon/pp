from __future__ import annotations

import re
from dataclasses import dataclass

from rapidfuzz import fuzz


@dataclass
class Country:
    code: str
    display_name: str
    names: list[str]
    visa_required: bool
    eaeu: bool = False


COUNTRIES: list[Country] = [
    Country(code="ARM", display_name="Армения", names=["armenia", "армен", "армения", "arm"], visa_required=False, eaeu=True),
    Country(code="KAZ", display_name="Казахстан", names=["kazakhstan", "каз", "казахстан", "kaz"], visa_required=False, eaeu=True),
    Country(code="KGZ", display_name="Кыргызстан", names=["kyrgyzstan", "кырг", "кыргызстан", "kg", "kgz"], visa_required=False, eaeu=True),
    Country(code="BLR", display_name="Беларусь", names=["belarus", "бел", "беларусь", "blr"], visa_required=False, eaeu=True),
    Country(code="UZB", display_name="Узбекистан", names=["uzbekistan", "узб", "узбекистан", "uzb"], visa_required=False, eaeu=False),
    Country(code="TJK", display_name="Таджикистан", names=["tajikistan", "тадж", "таджикистан", "tjk"], visa_required=True, eaeu=False),
    Country(code="UKR", display_name="Украина", names=["ukraine", "украин", "украина", "ukr"], visa_required=False, eaeu=False),
    Country(code="CHN", display_name="Китай", names=["china", "кит", "китай", "chn"], visa_required=True, eaeu=False),
    Country(code="IND", display_name="Индия", names=["india", "инд", "индия", "ind"], visa_required=True, eaeu=False),
]


def normalize(value: str) -> str:
    return re.sub(r"[^a-zа-я0-9]", "", value.lower())


def match_countries(query: str) -> tuple[Country | None, Country | None, int]:
    norm = normalize(query)
    scored: list[tuple[int, Country]] = []
    for country in COUNTRIES:
        best = max(fuzz.partial_ratio(norm, normalize(name)) for name in country.names)
        scored.append((best, country))
    scored.sort(key=lambda item: item[0], reverse=True)

    filtered = [(score, country) for score, country in scored if country.code != "RUS"]
    best_score, best_country = (filtered[0] if filtered else (0, None))
    accepted_country = best_country if best_score >= 50 else None
    return accepted_country, best_country, best_score


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
