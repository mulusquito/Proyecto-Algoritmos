#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, re, os

RAW_DIR = os.path.join(os.path.dirname(__file__), "raw_plain")
OUT_FILE = os.path.join(os.path.dirname(__file__), "articulos_borrador2.json")

ABSTRACT_HEADS = [r"A\s*B\s*S\s*T\s*R\s*A\s*C\s*T", r"\bAbstract\b"]
END_MARKERS = [
    r"\n\s*1\.\s*Introduction",
    r"\n\s*1\s+Introduction",
    r"\n\s*I\.\s*Introduction",
    r"\n\s*INTRODUCTION",
    r"\n\s*©\s*20",
    r"\nhttps?://doi\.org",
    r"\n\s*doi:\s*10\.",
    r"\n\s*\*\s*Corresponding author",
]


def read(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()


def window_after_abstract(text):
    starts = []
    for pat in ABSTRACT_HEADS:
        for m in re.finditer(pat, text):
            starts.append(m.end())
    if not starts:
        return None
    # probar cada punto de inicio (por si "abstract" aparece varias veces,
    # p.ej. en el titulo de una seccion "Graphical abstract")
    starts = sorted(set(starts))
    best = None
    for s in starts:
        tail = text[s:s + 8000]
        end = len(tail)
        for pat in END_MARKERS:
            m = re.search(pat, tail)
            if m and m.start() < end:
                end = m.start()
        window = tail[:end]
        if best is None or len(window) > len(best):
            best = window
    return best


def best_paragraph(window):
    if not window:
        return None
    # separar por lineas en blanco (parrafos)
    blocks = re.split(r"\n\s*\n", window)
    blocks = [b.strip() for b in blocks if b.strip()]
    if not blocks:
        return None
    # descartar bloques tipo "Keywords: a, b, c" o listas de palabras clave
    candidatos = []
    for b in blocks:
        low = b.lower()
        if low.startswith("keywords") or low.startswith("key words") or low.startswith("index terms"):
            continue
        if len(b) < 150:
            continue
        candidatos.append(b)
    pool = candidatos if candidatos else blocks
    pool.sort(key=len, reverse=True)
    return pool[0]


def clean(txt):
    if not txt:
        return ""
    txt = txt.replace("\r", "\n")
    txt = re.sub(r"-\n(?=[a-z])", "", txt)  # palabra cortada por guion de fin de linea
    txt = re.sub(r"\n+", " ", txt)
    txt = re.sub(r"\s{2,}", " ", txt)
    return txt.strip(" :•-\t")


def guess_title(text, filename):
    fallback = re.sub(r"^\d+\.\s*", "", filename).replace(".txt", "").strip()
    return fallback


def main():
    files = sorted(
        [f for f in os.listdir(RAW_DIR) if f.endswith(".txt")],
        key=lambda x: int(re.match(r"(\d+)\.", x).group(1)),
    )
    resultados = []
    for fn in files:
        num = int(re.match(r"(\d+)\.", fn).group(1))
        text = read(os.path.join(RAW_DIR, fn))
        window = window_after_abstract(text)
        parrafo = best_paragraph(window)
        abstract = clean(parrafo)
        resultados.append({
            "id": num,
            "archivo": fn.replace(".txt", ".pdf"),
            "titulo": guess_title(text, fn),
            "autores": "",
            "abstract": abstract,
            "abstract_len": len(abstract),
        })
    with open(OUT_FILE, "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=2)
    for r in resultados:
        estado = "OK" if r["abstract_len"] > 300 else "REVISAR"
        print(f"[{estado}] {r['id']:2d} len={r['abstract_len']:4d}  {r['archivo']}")


if __name__ == "__main__":
    main()
