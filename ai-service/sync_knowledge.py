"""
把 MySQL 里的知识同步到向量库

用法：
    python sync_knowledge.py            # 同步 FAQ
    python sync_knowledge.py --products # 同步商品（语义搜索用）
    python sync_knowledge.py --all      # 都同步

首次部署、或在后台增删改了 FAQ / 商品后需要跑一次。
"""

import argparse
import os
import sys

import pymysql
from dotenv import load_dotenv

from rag import rag

load_dotenv()

# 与 Java 端 application.yml 保持一致
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", "123456"),
    "database": os.getenv("DB_NAME", "esshop"),
    "charset": "utf8mb4",
}


def get_conn():
    return pymysql.connect(**DB_CONFIG)


def sync_faq() -> int:
    """同步客服知识库（chat_knowledge 表）"""
    sql = """
        SELECT id, category, question, keywords, answer
        FROM chat_knowledge
        WHERE deleted = 0 AND enabled = 1
    """
    with get_conn() as conn:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(sql)
            rows = cur.fetchall()

    docs = []
    for r in rows:
        # 把问题和关键词一起向量化，提升召回率
        # （用户可能问"怎么卖东西"，而原文是"怎么发布商品"）
        text = f"{r['question']}\n关键词：{r['keywords']}\n{r['answer']}"
        docs.append({
            "id": f"faq_{r['id']}",
            "text": text,
            "metadata": {
                "source": "faq",
                "db_id": r["id"],
                "question": r["question"],
                "category": r["category"] or "",
            },
        })

    n = rag.add_documents(docs, collection="faq", reset=True)
    print(f"FAQ 同步完成：{n} 条")
    return n


def sync_products() -> int:
    """同步商品（用于自然语言搜商品）"""
    # 关联分类表：分类名是重要的语义信号。
    # 例如"运动器材"能帮助"代步工具"这类泛化查询找到自行车。
    sql = """
        SELECT p.id, p.title, p.description, p.price, p.category_id,
               c.name AS category_name
        FROM product p
        LEFT JOIN category c ON p.category_id = c.id
        WHERE p.deleted = 0 AND p.status = 'ON_SALE'
    """
    with get_conn() as conn:
        with conn.cursor(pymysql.cursors.DictCursor) as cur:
            cur.execute(sql)
            rows = cur.fetchall()

    docs = []
    for r in rows:
        desc = (r["description"] or "")[:200]  # 描述太长会稀释语义
        cat = r["category_name"] or ""

        # 标题重复一次做加权：整段文本会被平均成一个向量，
        # 标题被描述等噪声稀释后核心词权重变低
        text = (
            f"商品名称：{r['title']}\n"
            f"名称：{r['title']}\n"
            f"分类：{cat}\n"
            f"价格：{r['price']}元\n"
            f"描述：{desc}"
        )
        docs.append({
            "id": f"product_{r['id']}",
            "text": text,
            "metadata": {
                "source": "product",
                "db_id": r["id"],
                "title": r["title"],
                "category": cat,
                "price": float(r["price"]),
            },
        })

    n = rag.add_documents(docs, collection="product", reset=True)
    print(f"商品同步完成：{n} 条")
    return n


def main():
    parser = argparse.ArgumentParser(description="同步知识到向量库")
    parser.add_argument("--products", action="store_true", help="只同步商品")
    parser.add_argument("--all", action="store_true", help="FAQ 和商品都同步")
    args = parser.parse_args()

    if not rag.init():
        print("向量库初始化失败，请确认 Ollama 已启动")
        sys.exit(1)

    if args.products:
        sync_products()
    elif args.all:
        sync_faq()
        sync_products()
    else:
        sync_faq()


if __name__ == "__main__":
    main()
