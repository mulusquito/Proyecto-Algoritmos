# Scripts de extracción y curación de datos

Scripts usados **una sola vez**, de forma offline, para producir los archivos de datos que consume la
aplicación (`app/src/main/resources/data/articulos.json` y `app/src/main/resources/embeddings/glove100_filtrado.tsv`).
No se ejecutan como parte de la aplicación en tiempo de ejecución; se documentan aquí por transparencia del
proceso (ver también `docs/DECLARACION_USO_IA.md`).

## Archivos

- **`extract2.py`** — extracción heurística inicial del texto de los 20 PDFs en `recursos/articulos-originales/`
  (usa `pdftotext` de `poppler-utils`): localiza la sección "Abstract" y separa el texto en bloques para
  identificar el abstract real.
- **`build_final.py`** — script de curación final: parte del resultado de `extract2.py`, corrige manualmente
  título/autores/abstract de los artículos donde la extracción automática no fue confiable (columnas del PDF,
  saltos de línea con guion suave, etc.), normaliza ligaduras tipográficas y guiones, y produce
  **`articulos.json`** (20 artículos con `id`, `archivo`, `titulo`, `autores`, `abstract`), que es el archivo
  copiado a `app/src/main/resources/data/articulos.json`.
- **`build_embeddings.py`** — descarga el modelo preentrenado GloVe 100 dimensiones (Wikipedia + Gigaword, vía
  `gensim`) y lo filtra para conservar únicamente los vectores de las palabras que aparecen en el vocabulario
  de los 20 abstracts, produciendo **`embeddings_glove100_filtrado.tsv`** (copiado a
  `app/src/main/resources/embeddings/glove100_filtrado.tsv`). El modelo completo (~170 MB) no se incluye en el
  repositorio; solo el subconjunto filtrado necesario (≈940 KB).

## Cómo reproducir (opcional)

```bash
pip install gensim
python3 extract2.py        # genera un borrador de extracción a partir de los PDFs
python3 build_final.py     # aplica las correcciones manuales y genera articulos.json
python3 build_embeddings.py  # descarga y filtra los embeddings GloVe
```

No es necesario volver a ejecutar estos scripts para correr la aplicación: los archivos de datos ya generados
están incluidos dentro de `app/src/main/resources/`.
