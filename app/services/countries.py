from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import List

from rapidfuzz import fuzz, process
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Country


@dataclass
class CountryMatch:
    country: Country
    score: float


class CountryService:
    def __init__(self, dataset_path: Path):
        self.dataset_path = dataset_path
        self.dataset: List[dict] = []

    def load_dataset(self) -> None:
        with self.dataset_path.open("r", encoding="utf-8") as fp:
            self.dataset = json.load(fp)

    async def sync(self, session: AsyncSession) -> None:
        existing = {row.code3 for row in (await session.scalars(select(Country))).all()}
        for country in self.dataset:
            if country["code3"] in existing:
                continue
            session.add(
                Country(
                    code3=country["code3"],
                    name_ru=country["name_ru"],
                    name_en=country["name_en"],
                    aliases=country.get("aliases", []),
                )
            )

    async def search(self, query: str, session: AsyncSession, limit: int = 5) -> List[CountryMatch]:
        query = query.strip()
        countries = (await session.scalars(select(Country))).all()
        choices: List[str] = []
        mapping: dict[str, Country] = {}
        for country in countries:
            labels: List[str] = [country.name_ru, country.name_en, country.code3] + list(country.aliases or [])
            for label in labels:
                key = f"{country.id}:{label}"
                mapping[key] = country
                choices.append(label)
        results: List[CountryMatch] = []
        for label, score, _ in process.extract(query, choices, scorer=fuzz.WRatio, limit=limit):
            for key, country in mapping.items():
                if label in key:
                    results.append(CountryMatch(country=country, score=score))
                    break
        unique: dict[int, CountryMatch] = {}
        for match in results:
            if match.country.id not in unique or unique[match.country.id].score < match.score:
                unique[match.country.id] = match
        return sorted(unique.values(), key=lambda m: m.score, reverse=True)[:limit]
