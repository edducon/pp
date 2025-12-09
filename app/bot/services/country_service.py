from __future__ import annotations

from dataclasses import dataclass
from datetime import timedelta
from typing import Iterable

from app.data.countries import COUNTRIES


@dataclass
class CountryRules:
    registration_days: int
    medical_days: int
    migration_card_duration: int
    visa_required: bool = False


COUNTRY_BY_CODE = {country["code"]: country for country in COUNTRIES}
EAEU_ALIASES: dict[str, list[str]] = {
    "ARM": ["армения", "armenia", "armenian", "арм"],
    "BLR": ["беларусь", "belarus", "blr", "rb"],
    "KAZ": ["казахстан", "kazakhstan", "kaz", "каз"],
    "KGZ": ["кыргызстан", "киргизия", "kyrgyzstan", "kg", "kgz"],
    "RUS": ["россия", "russia", "rus", "rf"],
}


def _names_for_country(country: dict) -> Iterable[str]:
    code = country["code"].lower()
    base = {country["name_en"].lower(), country["name_ru"].lower(), code}
    base.update(EAEU_ALIASES.get(country["code"], []))
    return base


def search_countries(query: str) -> list[tuple[str, str]]:
    normalized = query.lower().strip()
    if not normalized:
        return []
    matches: list[tuple[str, str]] = []
    for country in COUNTRIES:
        names = _names_for_country(country)
        if any(
            normalized in name or name.startswith(normalized) or normalized.startswith(name)
            for name in names
        ):
            display = country["name_ru"] or country["name_en"]
            matches.append((country["code"], display))
    return matches


def is_eaeu(code: str | None) -> bool:
    if not code:
        return False
    country = COUNTRY_BY_CODE.get(code)
    return bool(country and country.get("is_eaeu"))


def country_rules(settings, citizenship_code: str | None) -> CountryRules:
    if is_eaeu(citizenship_code):
        return CountryRules(
            settings.eaeu_registration_days,
            settings.eaeu_medical_days,
            settings.default_migration_card_duration_days,
            False,
        )
    return CountryRules(
        settings.non_eaeu_registration_days,
        settings.non_eaeu_medical_days,
        settings.default_migration_card_duration_days,
        True,
    )


def estimate_entry_date(migration_expiry, rules: CountryRules):
    return migration_expiry - timedelta(days=rules.migration_card_duration)


def get_country(code: str | None) -> dict | None:
    if not code:
        return None
    return COUNTRY_BY_CODE.get(code)
