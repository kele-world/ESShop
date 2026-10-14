"""
Provider 连通性测试

用法：
    python test_providers.py

打印当前 provider 配置并做连通性检查，配置的切换只改 .env 即可。
"""

import os
from dotenv import load_dotenv

load_dotenv()

from providers import get_embedding_provider, get_llm_provider, list_providers  # noqa: E402


def main():
    print("=" * 60)
    print("  Provider 配置")
    print("=" * 60)

    info = list_providers()
    print(f"\n  Embedding")
    print(f"    当前:  {info['embedding']['current']}")
    print(f"    模型:  {info['embedding']['model']}")
    print(f"    可选:  {info['embedding']['available']}")

    print(f"\n  LLM")
    print(f"    当前:  {info['llm']['current']}")
    print(f"    模型:  {info['llm']['model']}")
    print(f"    地址:  {info['llm']['base_url']}")
    print(f"    可选:  {info['llm']['available']}")

    print("\n" + "=" * 60)
    print("  连通性测试")
    print("=" * 60)

    emb = get_embedding_provider()
    print(f"\n[Embedding] {emb.info()}")
    if emb.health_check():
        vecs = emb.embed(["测试文本"])
        print(f"  通过，向量维度 {len(vecs[0]) if vecs else 0}")
    else:
        print("  不可用")

    llm = get_llm_provider()
    print(f"\n[LLM] {llm.info()}")
    print(f"  {'通过' if llm.health_check() else '不可用'}")

    print("\n当前 .env 配置：")
    for k in ("EMBEDDING_PROVIDER", "LLM_PROVIDER", "LLM_BASE_URL", "LLM_MODEL"):
        v = os.getenv(k, "(未设置)")
        if "KEY" in k and v:
            v = v[:10] + "***"
        print(f"    {k}={v}")


if __name__ == "__main__":
    main()
