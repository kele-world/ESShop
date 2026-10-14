"""
ESshop 智能客服服务

职责：
    - 用 LangChain 编排提示词与多轮对话记忆
    - 提供 HTTP 接口给 Java 后端调用
    - 出错时返回 degraded 标记，由 Java 降级到规则库

启动：
    pip install -r requirements.txt
    copy .env.example .env    # 填入 LLM_API_KEY
    python main.py
"""

import os
from contextlib import asynccontextmanager
from typing import Dict, List

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field

from providers import get_llm_provider, list_providers
from rag import rag

load_dotenv()

# ---- 大模型配置（全部可通过 .env 覆盖）----
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")
LLM_MODEL = os.getenv("LLM_MODEL", "deepseek-chat")
LLM_TEMPERATURE = float(os.getenv("LLM_TEMPERATURE", "0.7"))
LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "500"))

PORT = int(os.getenv("PORT", "8000"))

# 保留最近 N 轮对话，避免上下文无限增长
MAX_HISTORY_TURNS = 10

# 平台知识 —— 注入系统提示词，让模型了解 ESshop 的业务
SYSTEM_PROMPT = """你是 ESshop 校园二手交易平台的智能客服助手，名字叫「小E」。

## 你的职责
用友好、简洁、口语化的语气回答同学们关于校园二手交易的问题。

## 平台业务知识

**平台定位**：面向在校学生的二手物品交易平台，让闲置物品在校园内流动起来。

**商品模块**
- 发布商品：登录后点击右上角「发布」按钮，填写标题、分类、售价、描述，可上传最多 6 张图片
- 商品分类：电子产品、书籍教材、生活用品、服饰鞋帽、运动器材、数码配件、文具办公、其他
- 商品状态：在售（ON_SALE）、已售出（SOLD）、已下架（OFF_SHELF）
- 下架后商品在商品市场不可见，但在「我的发布」中仍能看到，可重新上架
- 支持快捷改价：在「我的发布」页面点击「改价」按钮

**订单模块**
- 下单流程：商品详情页点「立即购买」→ 在「我的订单」中付款
- 订单状态：待付款 → 已付款 → 已完成；待付款/已付款都可取消
- 取消订单后商品会自动重新上架
- 平台不提供在线支付，付款后需线下交易

**社区模块**
- 可以发布图文故事，需管理员审核通过后展示
- 支持分类：闲置好物、校园生活、二手心得、求助问答、其他
- 帖子支持点赞、收藏、评论、分享

**账号模块**
- 注册：用户名 3-20 字符，密码 6-20 字符
- 个人中心可修改头像、昵称、手机号
- 暂不支持自助找回密码，需联系管理员

**交易安全**
- 建议在校内公共场所（图书馆、食堂门口等）当面交易
- 当面验货后再付款，贵重物品建议结伴同行
- 不要提前转账给陌生人，不要脱离平台私下交易

## 回答要求
1. 优先使用上面的平台知识回答，不要编造平台没有的功能
2. 回答控制在 150 字以内，条目化、易读
3. 语气亲切，可以适当用「同学」「哦」「～」等口语化表达
4. 如果问题超出平台范围（如写代码、闲聊），礼貌说明你只能回答平台相关问题
5. 如果用户描述不清，可以主动追问细节
"""

# ==================== RAG 检索配置 ====================

# 是否启用 RAG（检索增强）。关闭则退回纯提示词模式
RAG_ENABLED = os.getenv("RAG_ENABLED", "true").lower() == "true"

# 检索条数与相似度阈值
RAG_TOP_K = int(os.getenv("RAG_TOP_K", "3"))
RAG_MIN_SCORE = float(os.getenv("RAG_MIN_SCORE", "0.45"))

# 检索到知识后，拼进提示词的模板
RAG_PROMPT_TEMPLATE = """以下是与用户问题相关的平台知识，请优先依据这些内容回答：
{context}

如果上述知识与问题无关，就按你自己的理解回答，不要生搬硬套。
"""


class ChatRequest(BaseModel):
    """Java 后端传来的请求"""
    session_id: str = Field(..., description="会话ID，用于隔离多轮对话记忆")
    user_id: int | None = Field(None, description="用户ID，未登录为 null")
    message: str = Field(..., description="用户消息")


class ChatResponse(BaseModel):
    """返回给 Java 后端的响应"""
    reply: str = Field(..., description="客服回复内容")
    degraded: bool = Field(False, description="是否降级（true 时 Java 应改用规则库）")
    reason: str | None = Field(None, description="降级原因")


# 内存态会话记忆：{session_id: [HumanMessage, AIMessage, ...]}
# 重启后清空；生产环境可换 Redis
_sessions: Dict[str, List] = {}

_llm: ChatOpenAI | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """启动时初始化 LLM 客户端（provider 由 .env 的 LLM_PROVIDER 决定）"""
    global _llm
    try:
        provider = get_llm_provider()
        print(f"[INFO] LLM provider: {provider.info()}")
        if not provider.health_check():
            print("[WARN] LLM 服务不可用，将以降级模式运行")
            _llm = None
        else:
            _llm = provider.create_client()
            print("[INFO] LLM 客户端初始化完成")
            print(f"       provider: {provider.name}")
            print(f"       模型:     {provider.model}")
            print(f"       地址:     {provider.base_url}")
    except Exception as exc:
        print(f"[WARN] LLM 初始化失败，将以降级模式运行: {exc}")
        _llm = None

    yield

    _sessions.clear()


app = FastAPI(title="ESshop 智能客服服务", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health():
    """健康检查 —— Java 后端启动时探测，返回当前使用的 provider"""
    try:
        providers = list_providers()
    except Exception as exc:
        providers = {"error": str(exc)}
    return {
        "status": "ok",
        "llm_ready": _llm is not None,
        "active_sessions": len(_sessions),
        "providers": providers,
    }


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    """处理一条用户消息，返回客服回复"""
    if _llm is None:
        return ChatResponse(
            reply="",
            degraded=True,
            reason="客服服务未配置 API Key",
        )

    message = req.message.strip()
    if not message:
        raise HTTPException(status_code=400, detail="消息内容不能为空")

    history = _sessions.setdefault(req.session_id, [])

    # 组装消息：系统提示词 (+ 检索到的知识) + 历史对话 + 本轮提问
    system_content = SYSTEM_PROMPT

    # 先从向量库检索相关平台知识，拼进系统提示词
    if RAG_ENABLED:
        try:
            hits = rag.search(message, collection="faq",
                              top_k=RAG_TOP_K, min_score=RAG_MIN_SCORE)
            if hits:
                context = "\n\n".join(
                    f"【{h['metadata'].get('question', '')}】\n{h['text']}" for h in hits
                )
                system_content = (
                    SYSTEM_PROMPT + "\n\n" + RAG_PROMPT_TEMPLATE.format(context=context)
                )
                print(f"[RAG] 命中 {len(hits)} 条，最高相似度 {hits[0]['score']}")
            else:
                print("[RAG] 未命中相关知识，使用纯提示词")
        except Exception as exc:
            # 检索失败不影响主流程，退回纯提示词
            print(f"[RAG] 检索失败，退回纯提示词: {exc}")

    messages = [SystemMessage(content=system_content)]
    messages.extend(history)
    messages.append(HumanMessage(content=message))

    try:
        response = await _llm.ainvoke(messages)
        reply = response.content.strip()
    except Exception as exc:
        # 网络超时、额度不足、Key 失效等 —— 交给 Java 降级到规则库
        print(f"[ERROR] 调用 LLM 失败: {exc}")
        return ChatResponse(
            reply="",
            degraded=True,
            reason=f"{type(exc).__name__}: {exc}",
        )

    # 记录本轮对话，超出上限则丢弃最早的
    history.append(HumanMessage(content=message))
    history.append(AIMessage(content=reply))
    if len(history) > MAX_HISTORY_TURNS * 2:
        del history[: len(history) - MAX_HISTORY_TURNS * 2]

    return ChatResponse(reply=reply, degraded=False)


@app.delete("/session/{session_id}")
async def clear_session(session_id: str):
    """清空指定会话的记忆"""
    _sessions.pop(session_id, None)
    return {"status": "ok"}


# ==================== 商品语义搜索 ====================


class ProductSearchRequest(BaseModel):
    """商品语义搜索请求"""
    query: str = Field(..., description="自然语言查询，如「有没有便宜的代步工具」")
    top_k: int = Field(10, description="返回条数")


@app.post("/search-products")
async def search_products(req: ProductSearchRequest):
    """
    自然语言搜索商品

    用户说"代步工具"，关键词搜不到"山地车"，这里通过向量检索 + 查询扩展能命中。
    返回商品 ID 列表（含相似度），由 Java 侧按 ID 查完整信息。
    """
    query = req.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="查询内容不能为空")

    try:
        hits = rag.search_multi(query, collection="product", top_k=req.top_k)
    except Exception as exc:
        print(f"[RAG] 商品检索失败: {exc}")
        return {"results": [], "degraded": True, "reason": str(exc)}

    results = [
        {
            "productId": int(h["metadata"]["db_id"]),
            "title": h["metadata"].get("title", ""),
            "price": h["metadata"].get("price", 0),
            "score": h["score"],
        }
        for h in hits
        if h.get("metadata", {}).get("db_id")
    ]
    print(f"[RAG] 商品搜索「{query}」返回 {len(results)} 条")
    return {"results": results, "degraded": False}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=PORT)
