#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Filtra el modelo GloVe (100d, preentrenado, descargado con gensim) para
quedarnos solo con los vectores de las palabras que realmente aparecen en
los 20 abstracts (tras normalizacion), y los guarda en un TSV compacto
que la aplicacion Java carga como recurso (sin necesitar Python/gensim
en tiempo de ejecucion).
"""
import json
import re
import os
from gensim.models import KeyedVectors

BASE = os.path.dirname(__file__)
STOPWORDS_EN = set("""
a about above after again against all am an and any are aren't as at be
because been before being below between both but by can't cannot could
couldn't did didn't do does doesn't doing don't down during each few for
from further had hadn't has hasn't have haven't having he he'd he'll he's
her here here's hers herself him himself his how how's i i'd i'll i'm i've
if in into is isn't it it's its itself let's me more most mustn't my myself
no nor not of off on once only or other ought our ours ourselves out over
own same shan't she she'd she'll she's should shouldn't so some such than
that that's the their theirs them themselves then there there's these they
they'd they'll they're they've this those through to too under until up
very was wasn't we we'd we'll we're we've were weren't what what's when
when's where where's which while who who's whom why why's with won't would
wouldn't you you'd you'll you're you've your yours yourself yourselves
""".split())


def tokenizar(texto):
    texto = texto.lower()
    palabras = re.findall(r"[a-z]+(?:'[a-z]+)?", texto)
    return [p for p in palabras if p not in STOPWORDS_EN and len(p) > 1]


def main():
    articulos = json.load(open(os.path.join(BASE, "articulos.json"), encoding="utf-8"))
    vocabulario = set()
    for art in articulos:
        vocabulario.update(tokenizar(art["abstract"]))
    print(f"Vocabulario (sin stopwords) tras preprocesamiento: {len(vocabulario)} palabras unicas")

    kv = KeyedVectors.load(os.path.join(BASE, "glove100.kv"))
    encontradas = 0
    out_path = os.path.join(BASE, "embeddings_glove100_filtrado.tsv")
    with open(out_path, "w", encoding="utf-8") as f:
        for palabra in sorted(vocabulario):
            if palabra in kv:
                vec = kv[palabra]
                f.write(palabra + "\t" + " ".join(f"{x:.6f}" for x in vec) + "\n")
                encontradas += 1
    faltantes = len(vocabulario) - encontradas
    print(f"Encontradas en GloVe: {encontradas} / {len(vocabulario)} (faltantes: {faltantes})")
    print(f"Archivo generado: {out_path}")
    print("Tamano:", os.path.getsize(out_path) / 1024, "KB")


if __name__ == "__main__":
    main()
