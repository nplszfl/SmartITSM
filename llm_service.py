"""
LLM Service - DeepSeek/GPT integration with retries for SmartITSM.
"""

import json
import logging
from typing import Optional

import httpx
from pydantic_settings import BaseSettings
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

logger = logging.getLogger(__name__)


class LLMSettings(BaseSettings):
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"
    llm_provider: str = "deepseek"
    request_timeout: int = 60

    class Config:
        env_prefix = "LLM_"


class LLMService:
    def __init__(self):
        self._settings = LLMSettings()
        self._http_client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._http_client is None:
            self._http_client = httpx.AsyncClient(timeout=self._settings.request_timeout)
        return self._http_client

    def _get_headers(self) -> dict:
        if self._settings.llm_provider == "deepseek":
            return {
                "Authorization": f"Bearer {self._settings.deepseek_api_key}",
                "Content-Type": "application/json",
            }
        else:
            return {
                "Authorization": f"Bearer {self._settings.openai_api_key}",
                "Content-Type": "application/json",
            }

    def _get_base_url(self) -> str:
        if self._settings.llm_provider == "deepseek":
            return self._settings.deepseek_base_url
        return self._settings.openai_base_url

    def _get_model(self) -> str:
        if self._settings.llm_provider == "deepseek":
            return self._settings.deepseek_model
        return self._settings.openai_model

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((httpx.TimeoutException, httpx.NetworkError)),
    )
    async def complete(self, prompt: str, system_prompt: Optional[str] = None, **kwargs) -> str:
        """Send a completion request to the LLM."""
        client = await self._get_client()
        base_url = self._get_base_url()
        model = self._get_model()

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": model,
            "messages": messages,
            "temperature": kwargs.get("temperature", 0.7),
            "max_tokens": kwargs.get("max_tokens", 2000),
        }

        try:
            response = await client.post(
                f"{base_url}/chat/completions",
                headers=self._get_headers(),
                json=payload,
            )
            response.raise_for_status()
            data = response.json()
            return data["choices"][0]["message"]["content"]
        except httpx.HTTPStatusError as e:
            logger.error(f"LLM HTTP error: {e.response.status_code} - {e.response.text}")
            raise
        except Exception as e:
            logger.error(f"LLM completion error: {e}")
            raise

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((httpx.TimeoutException, httpx.NetworkError)),
    )
    async def complete_json(self, prompt: str, system_prompt: Optional[str] = None, **kwargs) -> dict:
        """Send a completion request and parse JSON response."""
        text = await self.complete(prompt, system_prompt, **kwargs)
        try:
            text = text.strip()
            if text.startswith("```json"):
                text = text[7:]
            if text.startswith("```"):
                text = text[3:]
            if text.endswith("```"):
                text = text[:-3]
            return json.loads(text.strip())
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON: {e}\nResponse: {text}")
            raise ValueError(f"Invalid JSON response from LLM: {text[:200]}")

    async def close(self):
        if self._http_client:
            await self._http_client.aclose()
            self._http_client = None


llm_service = LLMService()