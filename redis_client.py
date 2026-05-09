"""
Redis Client for SmartITSM - Connection management and caching.
"""

import logging
from typing import Optional

import redis.asyncio as redis
from pydantic_settings import BaseSettings

logger = logging.getLogger(__name__)


class RedisSettings(BaseSettings):
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: Optional[str] = None
    redis_prefix: str = "smartitsm:"

    class Config:
        env_prefix = "REDIS_"


class RedisClient:
    def __init__(self):
        self._settings = RedisSettings()
        self._client: Optional[redis.Redis] = None

    async def connect(self):
        """Establish Redis connection."""
        try:
            self._client = redis.Redis(
                host=self._settings.redis_host,
                port=self._settings.redis_port,
                db=self._settings.redis_db,
                password=self._settings.redis_password,
                decode_responses=True,
            )
            await self._client.ping()
            logger.info(f"Connected to Redis at {self._settings.redis_host}:{self._settings.redis_port}")
        except Exception as e:
            logger.warning(f"Redis connection failed: {e}. Running without cache.")
            self._client = None

    async def disconnect(self):
        """Close Redis connection."""
        if self._client:
            await self._client.close()
            self._client = None

    def _key(self, key: str) -> str:
        return f"{self._settings.redis_prefix}{key}"

    async def get(self, key: str) -> Optional[str]:
        """Get value from cache."""
        if not self._client:
            return None
        try:
            return await self._client.get(self._key(key))
        except Exception as e:
            logger.error(f"Redis get error: {e}")
            return None

    async def set(self, key: str, value: str, expire: int = 3600) -> bool:
        """Set value in cache with expiration."""
        if not self._client:
            return False
        try:
            await self._client.set(self._key(key), value, ex=expire)
            return True
        except Exception as e:
            logger.error(f"Redis set error: {e}")
            return False

    async def delete(self, key: str) -> bool:
        """Delete key from cache."""
        if not self._client:
            return False
        try:
            await self._client.delete(self._key(key))
            return True
        except Exception as e:
            logger.error(f"Redis delete error: {e}")
            return False

    async def hget(self, key: str, field: str) -> Optional[str]:
        """Get hash field value."""
        if not self._client:
            return None
        try:
            return await self._client.hget(self._key(key), field)
        except Exception as e:
            logger.error(f"Redis hget error: {e}")
            return None

    async def hset(self, key: str, field: str, value: str, expire: int = 3600) -> bool:
        """Set hash field value."""
        if not self._client:
            return False
        try:
            await self._client.hset(self._key(key), field, value)
            await self._client.expire(self._key(key), expire)
            return True
        except Exception as e:
            logger.error(f"Redis hset error: {e}")
            return False


redis_client = RedisClient()