
import os
import re
import sqlite3
from typing import List

# =================================================
# ⚙️ 配置区
# =================================================
EXPORT_ROOT = r"E:\从数据库导出"  # txt 根目录下的volume_书籍
NEW_DB_PATH = r"E:\new1_xshj.db"
MIN_PARAGRAPH_LENGTH = 5  # 最短段落长度
MERGE_SHORT_LEN = 30  # 小于此长度的段落会合并到前一段

# =================================================
# 1. 建库 / 建表（只修改 paragraph 表）
# =================================================
def create_db():
    db_dir = os.path.dirname(NEW_DB_PATH)
    if db_dir and not os.path.exists(db_dir):
        os.makedirs(db_dir, exist_ok=True)
    if os.path.exists(NEW_DB_PATH):
        os.remove(NEW_DB_PATH)
        print("🗑️ 已删除旧数据库")

    conn = sqlite3.connect(NEW_DB_PATH)
    conn.execute('PRAGMA encoding = "UTF-8"')
    conn.execute('PRAGMA journal_mode = WAL')
    cur = conn.cursor()

    # 基础表（category, volume, chapter 完全不变）
    cur.executescript("""
    CREATE TABLE category(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        cateName TEXT NOT NULL
    );
    CREATE TABLE volume(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        volName TEXT NOT NULL,
        categoryId INTEGER NOT NULL
    );
    CREATE TABLE chapter(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        indexId INTEGER DEFAULT 0,
        name TEXT NOT NULL,
        volumeId INTEGER NOT NULL
    );
    CREATE TABLE paragraph(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        content TEXT NOT NULL,
        paragraph_index INTEGER DEFAULT 0,
        volume_id INTEGER NOT NULL,
        chapter_id INTEGER NOT NULL,
        category_id INTEGER NOT NULL,
        MusicLinks TEXT DEFAULT '',
        Second TEXT DEFAULT '',
        Type TEXT DEFAULT ''
    );
""")

    # FTS4 表（兼容旧版 SQLite）
    cur.execute("""
    CREATE VIRTUAL TABLE paragraph_fts USING fts4(
        content,
        tokenize=unicode61
    );
""")

    conn.commit()
    print("✅ 数据库创建完成（paragraph 表新增：MusicLinks, Second, Type）")
    return conn, cur

# =================================================
# 2. 工具函数（完全不变）
# =================================================
def safe_read(path: str) -> str:
    encodings = ['utf-8', 'gb2312', 'gbk', 'utf-8-sig']
    for enc in encodings:
        try:
            with open(path, "r", encoding=enc) as f:
                return f.read()
        except:
            continue
    print(f"⚠️ 无法读取文件: {path}")
    return ""

def clean_name(name: str) -> str:
    """去掉前缀编号"""
    return re.sub(r"^\d{1,3}[-_\s]", "", name).strip()

def insert_category(cur, name, cache):
    if name not in cache:
        cur.execute("INSERT INTO category(cateName) VALUES(?)", (name,))
        cache[name] = cur.lastrowid
    return cache[name]

# =================================================
# 3. 提取媒体信息（新增）
# =================================================
def extract_media_info(text: str):
    """从文本第一行提取媒体链接信息"""
    lines = text.splitlines()
    if not lines:
        return '', '', '', text

    first_line = lines[0].strip()

    # 判断第一行是否为 URL|秒数|类型 格式
    if '|' in first_line and (
            'http' in first_line or first_line.endswith('.mp3') or first_line.endswith('.mp4')):
        parts = first_line.split('|')
        if len(parts) >= 3:
            music_links = parts[0].strip()
            second = parts[1].strip()
            media_type = parts[2].strip()
            # 移除第一行，返回剩余文本
            remaining_text = '\n'.join(lines[1:])
            return music_links, second, media_type, remaining_text

    # 如果第一行不是链接，返回空值和原文本
    return '', '', '', text

# =================================================
# 4. 段落处理（⭐ 修改：保留 * 开头的行作为独立段落）
# =================================================
def split_into_paragraphs(text: str) -> List[str]:
    lines = [l.strip() for l in text.splitlines() if l.strip()]
    # ⭐ 直接返回所有非空行，每行作为一个段落
    return [line for line in lines if len(line) >= MIN_PARAGRAPH_LENGTH]

# =================================================
# 4. 核心处理（只修改 INSERT 语句）
# =================================================
def process_volume(cur, path, cache, parent_cate_name=""):
    for entry in sorted(os.listdir(path)):
        entry_path = os.path.join(path, entry)
        if not os.path.isdir(entry_path):
            continue

        txt_files = sorted([f for f in os.listdir(entry_path) if f.endswith(".txt")])
        if txt_files:
            # 书籍
            cate_id = insert_category(cur, parent_cate_name or "默认分类", cache)
            clean_vol_name = clean_name(entry)
            cur.execute("INSERT INTO volume(volName, categoryId) VALUES(?, ?)",
                        (clean_vol_name, cate_id))
            vol_id = cur.lastrowid
            print(f"\n📖 处理书籍: {clean_vol_name} ({len(txt_files)} 章)")

            for txt_idx, txt_file in enumerate(txt_files, 1):
                full_path = os.path.join(entry_path, txt_file)
                raw_content = safe_read(full_path)
                if not raw_content.strip():
                    continue

                idx = int(txt_file.split("-")[0]) if "-" in txt_file else txt_idx
                chapter_name = clean_name(
                    txt_file.split("-", 1)[-1].rsplit(".", 1)[0] if "-" in txt_file else
                    txt_file.rsplit(".", 1)[0])

                cur.execute("INSERT INTO chapter(indexId, name, volumeId) VALUES (?, ?, ?)",
                            (idx, chapter_name, vol_id))
                chapter_id = cur.lastrowid

                # ⭐ 提取章节级媒体信息
                music_links, second, media_type, cleaned_content = extract_media_info(raw_content)

                if music_links:
                    print(f"  🎵 发现媒体: {media_type} - {second}秒")

                paragraphs = split_into_paragraphs(cleaned_content)
                for para_idx, paragraph_content in enumerate(paragraphs, 1):
                    # ⭐ 只有第一个段落填充媒体信息
                    if para_idx == 1:
                        cur.execute("""
                        INSERT INTO paragraph(content, paragraph_index, volume_id, chapter_id, category_id,
                                              MusicLinks, Second, Type)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, (paragraph_content, para_idx, vol_id, chapter_id, cate_id,
                          music_links, second, media_type))
                    else:
                        cur.execute("""
                        INSERT INTO paragraph(content, paragraph_index, volume_id, chapter_id, category_id,
                                              MusicLinks, Second, Type)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, (paragraph_content, para_idx, vol_id, chapter_id, cate_id, '', '', ''))

                    cur.execute("INSERT INTO paragraph_fts(content) VALUES(?)", (paragraph_content,))
                    print(f"    ➕ 第{para_idx}段: {len(paragraph_content)}字")
        else:
            # 分类目录
            clean_entry = clean_name(entry)
            sub_cate_name = f"{parent_cate_name}/{clean_entry}" if parent_cate_name else clean_entry
            insert_category(cur, sub_cate_name, cache)
            process_volume(cur, entry_path, cache, sub_cate_name)

# =================================================
# 5. 主函数（完全不变）
# =================================================
def main():
    cache = {}
    conn, cur = create_db()
    base = os.path.join(EXPORT_ROOT, "volume_书籍")
    if not os.path.exists(base):
        os.makedirs(base, exist_ok=True)
        print(f"📂 已创建目录: {base}，请放入圣经文本")
        conn.close()
        return

    process_volume(cur, base, cache)
    conn.commit()
    conn.execute("VACUUM;")  # 压缩数据库
    print(f"\n🎉 数据库已保存: {NEW_DB_PATH}")
    conn.close()

if __name__ == "__main__":
    main()