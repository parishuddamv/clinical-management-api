import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    app_name: str = os.getenv("APP_NAME", "Gift Platform MVP")
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./gift_mvp.db")
    public_base_url: str = os.getenv("PUBLIC_BASE_URL", "http://localhost:8000")
    webhook_secret: str = os.getenv("WEBHOOK_SECRET", "dev_webhook_secret")
    claim_rate_limit_per_minute: int = int(os.getenv("CLAIM_RATE_LIMIT_PER_MINUTE", "30"))


settings = Settings()

