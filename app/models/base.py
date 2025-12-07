from datetime import datetime, timezone
from sqlalchemy.orm import DeclarativeBase, declared_attr
from sqlalchemy import MetaData


class Base(DeclarativeBase):
    metadata = MetaData()

    @declared_attr.directive
    def __tablename__(cls) -> str:  # type: ignore[override]
        return cls.__name__.lower()

    def __repr__(self) -> str:  # pragma: no cover - debug helper
        attrs = [f"{k}={getattr(self, k, None)!r}" for k in self.__mapper__.c.keys()]
        return f"{self.__class__.__name__}({', '.join(attrs)})"


def now_utc() -> datetime:
    return datetime.now(tz=timezone.utc)
