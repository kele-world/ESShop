"""
客服对话独立测试脚本

用法：
    python test_agent.py            # 交互式对话
    python test_agent.py --check    # 只测连通性
    python test_agent.py -m "怎么发布商品"   # 单次提问

无需启动 Java 后端和前端，直接测大模型这一步。
"""

import argparse
import sys

from dotenv import load_dotenv

load_dotenv()

import os  # noqa: E402

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage  # noqa: E402
from langchain_openai import ChatOpenAI  # noqa: E402

# 复用 main.py 里的配置和提示词，避免两处维护
from main import (
    DEEPSEEK_API_KEY,
    LLM_BASE_URL,
    LLM_MAX_TOKENS,
    LLM_MODEL,
    LLM_TEMPERATURE,
    SYSTEM_PROMPT,
)


def build_llm() -> ChatOpenAI:
    return ChatOpenAI(
        model=LLM_MODEL,
        api_key=DEEPSEEK_API_KEY,
        base_url=LLM_BASE_URL,
        temperature=LLM_TEMPERATURE,
        max_tokens=LLM_MAX_TOKENS,
        timeout=30,
    )


def check_connection(llm: ChatOpenAI) -> bool:
    """最小请求，验证 Key / 地址 / 模型名是否可用"""
    print("=" * 60)
    print("  连通性检查")
    print("=" * 60)
    print(f"  地址    : {LLM_BASE_URL}")
    print(f"  模型    : {LLM_MODEL}")
    key = DEEPSEEK_API_KEY or ""
    print(f"  API Key : {key[:12]}...{key[-6:]}" if len(key) > 20 else f"  API Key : {key or '(未配置)'}")
    print("-" * 60)

    if not key or key.startswith("sk-xxx"):
        print("  [失败] 未配置 API Key，请在 .env 中填写 DEEPSEEK_API_KEY")
        return False

    try:
        resp = llm.invoke([HumanMessage(content="回复两个字：正常")])
        print(f"  [成功] 模型响应: {resp.content.strip()}")
        return True
    except Exception as exc:
        print(f"  [失败] {type(exc).__name__}: {exc}")
        print()
        print("  常见原因：")
        print("   - 401/403 : API Key 错误或已失效")
        print("   - 404     : base_url 或模型名不对（中转站常要求特定模型名）")
        print("   - 超时    : 网络不通，检查是否能访问中转站地址")
        print("   - 余额    : 账户额度不足")
        return False


def ask(llm: ChatOpenAI, question: str) -> str:
    """单轮提问（带系统提示词）"""
    messages = [
        SystemMessage(content=SYSTEM_PROMPT),
        HumanMessage(content=question),
    ]
    return llm.invoke(messages).content.strip()


def interactive(llm: ChatOpenAI):
    """交互式多轮对话 —— 内存中维护历史，模拟线上行为"""
    print()
    print("=" * 60)
    print("  交互式对话模式")
    print("  输入问题回车发送；输入 /clear 清空记忆；/exit 退出")
    print("=" * 60)
    print()

    history = []
    while True:
        try:
            question = input("你 > ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n再见～")
            break

        if not question:
            continue
        if question in ("/exit", "/quit"):
            print("再见～")
            break
        if question == "/clear":
            history.clear()
            print("[已清空对话记忆]\n")
            continue

        messages = [SystemMessage(content=SYSTEM_PROMPT)] + history + [HumanMessage(content=question)]
        try:
            reply = llm.invoke(messages).content.strip()
        except Exception as exc:
            print(f"小E > [错误] {type(exc).__name__}: {exc}\n")
            continue

        print(f"小E > {reply}\n")

        # 维护最近 10 轮
        history.append(HumanMessage(content=question))
        history.append(AIMessage(content=reply))
        if len(history) > 20:
            del history[: len(history) - 20]


def main():
    parser = argparse.ArgumentParser(description="ESshop 智能客服测试工具")
    parser.add_argument("--check", action="store_true", help="只做连通性检查")
    parser.add_argument("-m", "--message", type=str, help="单次提问")
    args = parser.parse_args()

    llm = build_llm()
    ok = check_connection(llm)

    if args.check:
        sys.exit(0 if ok else 1)

    if not ok:
        sys.exit(1)

    if args.message:
        print()
        print(f"你 > {args.message}")
        print(f"小E > {ask(llm, args.message)}")
        return

    interactive(llm)


if __name__ == "__main__":
    main()
