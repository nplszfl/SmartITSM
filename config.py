"""
Configuration management for SmartITSM AI Worker.
Loads settings from config.yaml and environment variables.
"""

import os
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # LLM Settings
    llm_provider: str = "deepseek"
    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"
    request_timeout: int = 60

    # Redis Settings
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: Optional[str] = None
    redis_prefix: str = "smartitsm:"

    # Server Settings
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"

    class Config:
        env_prefix = ""


def load_config(config_path: str = "config.yaml") -> Settings:
    """Load configuration from YAML file and environment variables."""
    settings = Settings()
    
    # Try to load from config.yaml if it exists
    if os.path.exists(config_path):
        try:
            import yaml
            with open(config_path, 'r') as f:
                config_data = yaml.safe_load(f)
                if config_data:
                    for key, value in config_data.items():
                        if hasattr(settings, key):
                            setattr(settings, key, value)
        except ImportError:
            print("PyYAML not installed, using environment variables only")
        except Exception as e:
            print(f"Error loading config.yaml: {e}")
    
    return settings


# Default settings instance
settings = Settings()