from pathlib import Path
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    bot_token: str = Field(..., alias="BOT_TOKEN")
    database_url: str = Field(..., alias="DATABASE_URL")
    superadmin_id: int = Field(..., alias="SUPERADMIN_ID")
    timezone: str = Field("Europe/Moscow", alias="TIMEZONE")
    log_file: Path = Field(Path("logs/bot.log"), alias="LOG_FILE")
    scheduler_interval_minutes: int = Field(60, alias="SCHEDULER_INTERVAL_MINUTES")
    default_notification_start: str = Field("09:00", alias="DEFAULT_NOTIFY_START")
    default_notification_end: str = Field("22:00", alias="DEFAULT_NOTIFY_END")
    default_migration_card_duration_days: int = Field(60, alias="DEFAULT_MIGRATION_CARD_DURATION_DAYS")
    eaeu_registration_days: int = Field(30, alias="EAEU_REGISTRATION_DAYS")
    eaeu_medical_days: int = Field(30, alias="EAEU_MEDICAL_DAYS")
    non_eaeu_registration_days: int = Field(10, alias="NON_EAEU_REGISTRATION_DAYS")
    non_eaeu_medical_days: int = Field(30, alias="NON_EAEU_MEDICAL_DAYS")
    visa_renewal_notice_days: int = Field(45, alias="VISA_RENEWAL_NOTICE_DAYS")


def load_settings() -> Settings:
    return Settings()
