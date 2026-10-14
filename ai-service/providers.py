"""
模型 Provider 抽象层

- Embedding: ollama（本地）/ openai（云端兼容接口）
- LLM:       openai（兼容 OpenAI 协议）/ ollama（本地模型）

通过 .env 的 EMBEDDING_PROVIDER / LLM_PROVIDER 切换，避免改代码。
"""

import os
from abc import ABC, abstractmethod
from typing import List

import requests
from dotenv import load_dotenv

load_dotenv()


# ==================== 配置读取 ====================

def _env(key: str, default: str = "") -> str:
    return os.getenv(key, default)


def _env_int(key: str, default: int) -> int:
    try:
        return int(os.getenv(key, str(default)))
    except ValueError:
        return default


def _env_float(key: str, default: float) -> float:
    try:
        return float(os.getenv(key, str(default)))
    except ValueError:
        return default


# ==================== Embedding Provider ====================

class EmbeddingProvider(ABC):
    """Embedding 提供者抽象基类"""

    @property
    @abstractmethod
    def name(self) -> str:
        """provider 名称，用于日志"""

    @property
    @abstractmethod
    def model(self) -> str:
        """模型名"""

    @abstractmethod
    def embed(self, texts: List[str]) -> List[List[float]]:
        """把一批文本转成向量"""

    @abstractmethod
    def health_check(self) -> bool:
        """检查服务是否可用"""

    def info(self) -> str:
        return f"{self.name} / {self.model}"


class OllamaEmbedding(EmbeddingProvider):
    """Ollama 本地 embedding"""

    def __init__(self) -> None:
        self._url = _env("OLLAMA_URL", "http://localhost:11434")
        self._model = _env("EMBEDDING_MODEL", "qwen3-embedding:4b")
        self._timeout = _env_int("EMBED_TIMEOUT", 120)

    @property
    def name(self) -> str:
        return "ollama"

    @property
    def model(self) -> str:
        return self._model

    def embed(self, texts: List[str]) -> List[List[float]]:
        if not texts:
            return []
        resp = requests.post(
            f"{self._url}/api/embed",
            json={"model": self._model, "input": texts},
            timeout=self._timeout,
        )
        resp.raise_for_status()
        return resp.json().get("embeddings", [])

    def health_check(self) -> bool:
        try:
            resp = requests.get(f"{self._url}/api/tags", timeout=5)
            resp.raise_for_status()
            names = [m.get("name", "") for m in resp.json().get("models", [])]
            # Ollama 模型名可能带 :latest 后缀，按前缀匹配
            base = self._model.split(":")[0]
            if not any(n.startswith(base) for n in names):
                print(f"[Embedding] Ollama 未找到模型 {self._model}")
                print(f"            请执行: ollama pull {self._model}")
                return False
            return True
        except Exception as exc:
            print(f"[Embedding] Ollama 不可用: {exc}")
            return False


class OpenAIEmbedding(EmbeddingProvider):
    """OpenAI 兼容接口的 embedding，适用于官方 OpenAI、各类中转站等"""

    def __init__(self) -> None:
        self._url = _env("EMBEDDING_BASE_URL", "https://api.openai.com/v1").rstrip("/")
        self._key = _env("EMBEDDING_API_KEY", "")
        self._model = _env("EMBEDDING_MODEL", "text-embedding-3-small")
        self._timeout = _env_int("EMBED_TIMEOUT", 60)

    @property
    def name(self) -> str:
        return "openai"

    @property
    def model(self) -> str:
        return self._model

    def embed(self, texts: List[str]) -> List[List[float]]:
        if not texts:
            return []
        resp = requests.post(
            f"{self._url}/embeddings",
            headers={
                "Authorization": f"Bearer {self._key}",
                "Content-Type": "application/json",
            },
            json={"model": self._model, "input": texts},
            timeout=self._timeout,
        )
        resp.raise_for_status()
        data = resp.json().get("data", [])
        # 按 index 排序，保证顺序和输入一致
        data.sort(key=lambda x: x.get("index", 0))
        return [d["embedding"] for d in data]

    def health_check(self) -> bool:
        if not self._key:
            print("[Embedding] 未配置 EMBEDDING_API_KEY")
            return False
        try:
            self.embed(["test"])
            return True
        except Exception as exc:
            print(f"[Embedding] OpenAI 兼容接口不可用: {exc}")
            return False


# ==================== LLM Provider ====================

class LLMProvider(ABC):
    """大模型提供者抽象基类"""

    @property
    @abstractmethod
    def name(self) -> str:
        """provider 名称"""

    @property
    @abstractmethod
    def model(self) -> str:
        """模型名"""

    @property
    @abstractmethod
    def base_url(self) -> str:
        """接口地址"""

    @abstractmethod
    def create_client(self):
        """创建 LangChain 的 Chat 客户端"""

    def health_check(self) -> bool:
        """默认实现：试调一次"""
        try:
            self.create_client().invoke("hi")
            return True
        except Exception as exc:
            print(f"[LLM] {self.name} 不可用: {exc}")
            return False

    def info(self) -> str:
        return f"{self.name} / {self.model} @ {self.base_url}"


class OpenAILLM(LLMProvider):
    """
    OpenAI 协议兼容的大模型

    覆盖 DeepSeek 官方、API 中转站、通义千问、智谱 GLM、Ollama 本地等
    """

    def __init__(self) -> None:
        self._url = _env("LLM_BASE_URL", "https://api.deepseek.com/v1").rstrip("/")
        self._key = _env("LLM_API_KEY") or _env("DEEPSEEK_API_KEY", "")
        self._model = _env("LLM_MODEL", "deepseek-chat")
        self._temperature = _env_float("LLM_TEMPERATURE", 0.3)
        self._max_tokens = _env_int("LLM_MAX_TOKENS", 1000)
        self._timeout = _env_int("LLM_REQUEST_TIMEOUT", 60)

    @property
    def name(self) -> str:
        return "openai"

    @property
    def model(self) -> str:
        return self._model

    @property
    def base_url(self) -> str:
        return self._url

    def create_client(self):
        from langchain_openai import ChatOpenAI
        return ChatOpenAI(
            model=self._model,
            api_key=self._key,
            base_url=self._url,
            temperature=self._temperature,
            max_tokens=self._max_tokens,
            timeout=self._timeout,
        )


class OllamaLLM(LLMProvider):
    """Ollama 本地大模型（/v1 是 OpenAI 兼容层，实现与 OpenAILLM 一致）"""

    def __init__(self) -> None:
        self._url = _env("OLLAMA_LLM_URL", "http://localhost:11434/v1").rstrip("/")
        self._model = _env("OLLAMA_LLM_MODEL", "qwen2.5:7b")
        self._temperature = _env_float("LLM_TEMPERATURE", 0.3)
        self._max_tokens = _env_int("LLM_MAX_TOKENS", 1000)
        self._timeout = _env_int("LLM_REQUEST_TIMEOUT", 120)

    @property
    def name(self) -> str:
        return "ollama"

    @property
    def model(self) -> str:
        return self._model

    @property
    def base_url(self) -> str:
        return self._url

    def create_client(self):
        from langchain_openai import ChatOpenAI
        # Ollama 不校验 Key，但要传一个非空值
        return ChatOpenAI(
            model=self._model,
            api_key="ollama",
            base_url=self._url,
            temperature=self._temperature,
            max_tokens=self._max_tokens,
            timeout=self._timeout,
        )

    def health_check(self) -> bool:
        try:
            resp = requests.get(
                self._url.replace("/v1", "") + "/api/tags", timeout=5)
            resp.raise_for_status()
            names = [m.get("name", "") for m in resp.json().get("models", [])]
            base = self._model.split(":")[0]
            if not any(n.startswith(base) for n in names):
                print(f"[LLM] Ollama 未找到模型 {self._model}")
                print(f"      请执行: ollama pull {self._model}")
                return False
            return True
        except Exception as exc:
            print(f"[LLM] Ollama 不可用: {exc}")
            return False


# ==================== 工厂函数 ====================

_EMBEDDING_PROVIDERS = {
    "ollama": OllamaEmbedding,
    "openai": OpenAIEmbedding,
}

_LLM_PROVIDERS = {
    "openai": OpenAILLM,
    "ollama": OllamaLLM,
}

_embedding_instance: EmbeddingProvider | None = None
_llm_instance: LLMProvider | None = None


def get_embedding_provider() -> EmbeddingProvider:
    """获取 embedding provider（单例），通过 .env 的 EMBEDDING_PROVIDER 选择"""
    global _embedding_instance
    if _embedding_instance is not None:
        return _embedding_instance

    key = _env("EMBEDDING_PROVIDER", "ollama").lower()
    cls = _EMBEDDING_PROVIDERS.get(key)
    if cls is None:
        print(f"[Embedding] 未知的 provider '{key}'，可用: {list(_EMBEDDING_PROVIDERS)}")
        print(f"            已回退到 ollama")
        cls = OllamaEmbedding

    _embedding_instance = cls()
    return _embedding_instance


def get_llm_provider() -> LLMProvider:
    """获取 LLM provider（单例），通过 .env 的 LLM_PROVIDER 选择"""
    global _llm_instance
    if _llm_instance is not None:
        return _llm_instance

    key = _env("LLM_PROVIDER", "openai").lower()
    cls = _LLM_PROVIDERS.get(key)
    if cls is None:
        print(f"[LLM] 未知的 provider '{key}'，可用: {list(_LLM_PROVIDERS)}")
        print(f"      已回退到 openai")
        cls = OpenAILLM

    _llm_instance = cls()
    return _llm_instance


def list_providers() -> dict:
    """列出可用的 provider（供 /health 接口展示）"""
    emb = get_embedding_provider()
    llm = get_llm_provider()
    return {
        "embedding": {
            "current": emb.name,
            "model": emb.model,
            "available": list(_EMBEDDING_PROVIDERS.keys()),
        },
        "llm": {
            "current": llm.name,
            "model": llm.model,
            "base_url": llm.base_url,
            "available": list(_LLM_PROVIDERS.keys()),
        },
    }


# ==================== 自测 ====================

if __name__ == "__main__":
    print("=" * 60)
    print("  Provider 配置检查")
    print("=" * 60)
    info = list_providers()
    print(f"\n  Embedding: {info['embedding']['current']} / {info['embedding']['model']}")
    print(f"             可选: {info['embedding']['available']}")
    print(f"\n  LLM:       {info['llm']['current']} / {info['llm']['model']}")
    print(f"             地址: {info['llm']['base_url']}")
    print(f"             可选: {info['llm']['available']}")

    print("\n" + "-" * 60)
    print("  连通性测试")
    print("-" * 60)

    emb = get_embedding_provider()
    print(f"\n[Embedding] {emb.info()}")
    ok = emb.health_check()
    print(f"  健康检查: {'通过' if ok else '失败'}")
    if ok:
        vecs = emb.embed(["测试文本"])
        if vecs:
            print(f"  向量维度: {len(vecs[0])}")

    llm = get_llm_provider()
    print(f"\n[LLM] {llm.info()}")
    ok = llm.health_check()
    print(f"  健康检查: {'通过' if ok else '失败'}")
