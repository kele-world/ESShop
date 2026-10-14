"""
RAG 检索模块

1. 把文本向量化（embedding，provider 见 providers.py）
2. 存入 Chroma 向量库（本地持久化）
3. 按语义相似度检索相关片段
"""

import os
from typing import Dict, List, Optional

import chromadb
from chromadb.config import Settings

from providers import get_embedding_provider

# 向量库持久化目录
CHROMA_DIR = os.path.join(os.path.dirname(__file__), "chroma_db")


class RagStore:
    """向量库封装：向量化 + 存储 + 语义检索"""

    def __init__(self) -> None:
        self._client: Optional[chromadb.ClientAPI] = None
        self._ready = False

    # ==================== 初始化 ====================

    def init(self) -> bool:
        """初始化向量库并探测 embedding 服务；失败时上层降级到关键词匹配"""
        if self._ready:
            return True
        try:
            provider = get_embedding_provider()
            print(f"[RAG] Embedding provider: {provider.info()}")
            if not provider.health_check():
                print("[RAG] Embedding 服务不可用，将降级到关键词匹配")
                self._ready = False
                return False

            print(f"[RAG] 打开向量库: {CHROMA_DIR}")
            self._client = chromadb.PersistentClient(
                path=CHROMA_DIR,
                settings=Settings(anonymized_telemetry=False, allow_reset=True),
            )
            self._ready = True
            print(f"[RAG] 初始化完成，embedding: {provider.model}")
            return True
        except Exception as exc:
            print(f"[RAG] 初始化失败，将降级到关键词匹配: {exc}")
            self._ready = False
            return False

    @property
    def ready(self) -> bool:
        return self._ready

    # ==================== 向量化 ====================

    def embed(self, texts: List[str]) -> List[List[float]]:
        """把一批文本转成向量（具体模型由 providers.py 按 .env 决定）"""
        if not texts:
            return []
        provider = get_embedding_provider()
        return provider.embed(texts)

    # ==================== 写入 ====================

    def add_documents(self, documents: List[Dict], collection: str = "faq",
                      reset: bool = False) -> int:
        """批量写入文档（upsert，支持增量同步）"""
        if not self.init():
            return 0

        if reset:
            try:
                self._client.delete_collection(collection)
                print(f"[RAG] 已清空集合 {collection}")
            except Exception:
                pass

        if not documents:
            return 0

        col = self._client.get_or_create_collection(
            name=collection,
            metadata={"hnsw:space": "cosine"},
        )

        texts = [d["text"] for d in documents]
        ids = [str(d["id"]) for d in documents]
        metadatas = [d.get("metadata", {}) for d in documents]

        embeddings = self.embed(texts)
        if len(embeddings) != len(texts):
            print(f"[RAG] 向量数量不匹配: {len(embeddings)} != {len(texts)}")
            return 0

        col.upsert(ids=ids, documents=texts, embeddings=embeddings, metadatas=metadatas)
        print(f"[RAG] 写入 {len(documents)} 条到 {collection}")
        return len(documents)

    def delete(self, ids: List[str], collection: str = "faq") -> None:
        """按 ID 删除（知识库删条目时同步）"""
        if not self.init():
            return
        try:
            self._client.get_collection(collection).delete(ids=[str(i) for i in ids])
        except Exception as exc:
            print(f"[RAG] 删除失败: {exc}")

    # ==================== 检索 ====================

    def search(self, query: str, collection: str = "faq",
               top_k: int = 3, min_score: float = 0.0,
               keyword_weight: float = 0.0) -> List[Dict]:
        """
        混合检索：向量相似度 + 关键词加分

        :param keyword_weight: 关键词权重。0 表示纯向量检索。
                               大于 0 时，标题/文本中包含查询词的候选会被加分，
                               用于缓解「同义词查询」失准（如搜"代步工具"命中"山地车"，
                               只要描述里提到了相关词）。
        """
        if not self.init() or not query.strip():
            return []

        try:
            col = self._client.get_collection(collection)
        except Exception:
            return []

        if col.count() == 0:
            return []

        embeddings = self.embed([query])
        if not embeddings:
            return []

        # 多召回一些候选，混合打分后再截断，避免关键词命中的条目落在召回窗口外
        recall_n = min(max(top_k * 3, 10), col.count())

        result = col.query(
            query_embeddings=embeddings,
            n_results=recall_n,
            include=["documents", "metadatas", "distances"],
        )

        docs = result.get("documents", [[]])[0]
        metas = result.get("metadatas", [[]])[0]
        dists = result.get("distances", [[]])[0]
        ids = result.get("ids", [[]])[0]

        # 从查询中剔除常见疑问词和停用词，保留有实义的片段用于加分
        keywords = self._extract_keywords(query)

        items = []
        for i, doc in enumerate(docs):
            vector_score = 1 - dists[i] if i < len(dists) else 0   # 余弦距离 → 相似度

            bonus = 0.0
            if keyword_weight > 0 and keywords:
                text = (doc or "").lower()
                hit_len = sum(len(k) for k in keywords if k in text)
                if hit_len:
                    # 命中长度占查询总长的比例，作为加分系数
                    coverage = hit_len / max(sum(len(k) for k in keywords), 1)
                    bonus = keyword_weight * min(coverage, 1.0)

            final_score = min(vector_score + bonus, 1.0)
            if final_score < min_score:
                continue
            items.append({
                "id": ids[i],
                "text": doc,
                "score": round(final_score, 4),
                "vector_score": round(vector_score, 4),
                "metadata": metas[i] if i < len(metas) else {},
            })

        # 按混合分数重排后截断
        items.sort(key=lambda x: x["score"], reverse=True)
        return items[:top_k]

    # 查询里常见的无实义词，提取关键词时剔除
    _STOPWORDS = ("有没有", "我想买", "我想", "买个", "一个", "怎么办", "怎么", "什么",
                  "哪里", "可以", "能不能", "想要", "求", "的", "了", "呢", "吗", "啊")

    def _extract_keywords(self, query: str) -> List[str]:
        """从查询里粗略提取关键词：剔除停用词 + 按标点切分，不引入 jieba"""
        import re
        text = query.lower()
        for w in self._STOPWORDS:
            text = text.replace(w, " ")
        # 按标点和空格切分，保留长度 >= 2 的片段
        parts = re.split(r"[\s,，。、！？!?；;：:]+", text)
        return [p for p in parts if len(p) >= 2]

    # ==================== 查询扩展 ====================

    # 口语化查询 → 具体品类词的映射表。
    # 用户说"代步工具"但商品标题写"山地车"，字面不重叠，纯向量检索会失准。
    # 检索时把泛化说法扩展成具体品类词一起搜。生产环境可用 LLM 动态改写，
    # 这里用静态表：零成本、无延迟，且校园二手场景的品类相对固定。
    _QUERY_EXPANSIONS = {
        "代步工具": "自行车 电动车 滑板车 平衡车",
        "代步": "自行车 电动车 滑板车",
        "交通工具": "自行车 电动车 滑板车",
        "写论文": "笔记本 平板 iPad 电脑",
        "办公": "鼠标 键盘 显示器 笔记本",
        "数码": "手机 平板 电脑 耳机",
        "学习": "教材 书籍 笔记 真题",
        "考试": "真题 教材 复习 考研",
        "考研": "真题 教材 复习",
        "听歌": "耳机 音响 音乐",
        "拍照": "相机 手机",
        "打游戏": "电脑 游戏机 手柄",
        "睡觉": "床垫 被子 枕头",
        "看书": "台灯 书架 书籍",
        "喝水": "水杯 保温杯",
    }

    def _expand_query(self, query: str) -> List[str]:
        """把查询扩展成变体：原查询 + 原查询拼接具体品类词"""
        variants = [query]
        for colloquial, concrete in self._QUERY_EXPANSIONS.items():
            if colloquial in query:
                # 保留原查询的修饰词（如"便宜的"），拼上具体品类
                variants.append(f"{query} {concrete}")
                break  # 命中一个就够，避免扩展过度导致语义漂移
        return variants

    def search_multi(
        self,
        query: str,
        collection: str = "product",
        top_k: int = 3,
        min_score: float = 0.0,
    ) -> List[Dict]:
        """
        带查询扩展的检索：对原查询和扩展查询各检索一次，合并去重。

        泛化的原始查询（如"便宜的代步工具"）分数普遍虚高，
        合并时扩展变体的权重高于原始查询，避免泛化词压过具体词。
        """
        variants = self._expand_query(query)

        # 原查询权重 0.7，扩展变体权重 1.0
        weights = [0.7] + [1.0] * (len(variants) - 1)

        merged: Dict[str, Dict] = {}
        for q, w in zip(variants, weights):
            for hit in self.search(q, collection=collection, top_k=top_k * 2,
                                   min_score=min_score):
                weighted = hit["score"] * w
                # 同一商品多次命中时保留最高加权分
                old = merged.get(hit["id"])
                if old is None or weighted > old["score"]:
                    # 记录加权后的分数用于排序，保留原始分数用于展示
                    merged[hit["id"]] = {**hit, "score": weighted,
                                         "raw_score": hit["score"]}

        items = sorted(merged.values(), key=lambda x: x["score"], reverse=True)
        return items[:top_k]

    def count(self, collection: str = "faq") -> int:
        if not self.init():
            return 0
        try:
            return self._client.get_collection(collection).count()
        except Exception:
            return 0

    def stats(self) -> Dict:
        """向量库概览，供健康检查用"""
        if not self.init():
            return {"ready": False}
        provider = get_embedding_provider()
        result: Dict = {
            "ready": True,
            "provider": provider.name,
            "model": provider.model,
            "collections": {},
        }
        try:
            for col in self._client.list_collections():
                name = col if isinstance(col, str) else col.name
                result["collections"][name] = self.count(name)
        except Exception as exc:
            result["error"] = str(exc)
        return result


rag = RagStore()