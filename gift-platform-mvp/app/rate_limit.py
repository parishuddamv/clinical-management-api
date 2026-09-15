from collections import defaultdict, deque
from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, Request


class InMemoryRateLimiter:
    def __init__(self, max_hits_per_minute: int):
        self.max_hits_per_minute = max_hits_per_minute
        self._hits: dict[str, deque[datetime]] = defaultdict(deque)

    def check(self, key: str) -> None:
        now = datetime.now(timezone.utc)
        window_start = now - timedelta(minutes=1)
        bucket = self._hits[key]

        while bucket and bucket[0] < window_start:
            bucket.popleft()

        if len(bucket) >= self.max_hits_per_minute:
            raise HTTPException(status_code=429, detail="Too many requests. Try again in a minute.")

        bucket.append(now)


def get_client_ip(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "unknown"

