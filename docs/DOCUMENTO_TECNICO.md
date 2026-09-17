# Documento Técnico de Diseño

**Proyecto:** Análisis de algoritmos en el contexto de la bibliometría
**Institución:** Universidad del Quindío — Programa de Ingeniería de Sistemas y Computación
**Dominio de conocimiento:** Inteligencia Artificial Generativa en Educación (20 artículos científicos)

---

## 1. Introducción y alcance

Este documento describe el diseño, la arquitectura y el análisis algorítmico-matemático de la solución
implementada para el proyecto. El sistema procesa los **abstracts** de 20 artículos científicos suministrados
por el docente, y responde a dos requerimientos funcionales:

- **Requerimiento 1:** análisis de similitud textual entre pares de artículos, usando 4 algoritmos clásicos y
  2 algoritmos basados en Inteligencia Artificial.
- **Requerimiento 2:** agrupamiento jerárquico de los 20 abstracts usando 3 criterios de enlace distintos,
  representado como dendrograma, y evaluado con una métrica de coherencia interna.

De acuerdo con la nota del enunciado ("la presente descripción del proyecto puede ser modificada [...] en
particular con los requerimientos funcionales"), este documento cubre los requerimientos 1, 2 y 6
(despliegue + documentación) tal como estaban especificados al momento de desarrollar el proyecto. La
arquitectura se diseñó de forma modular (ver sección 2) para poder incorporar requerimientos adicionales sin
reescribir el sistema.

Todos los algoritmos de similitud y de clustering fueron **implementados explícitamente con estructuras
básicas del lenguaje** (arreglos, mapas, recursión), sin usar funciones de alto nivel de una librería que
resuelva directamente el problema, conforme lo exige el enunciado.

---

## 2. Arquitectura del sistema

### 2.1 Vista general

```
                     ┌─────────────────────────────┐
                     │   Navegador (frontend)       │
                     │  index.html + app.js + CSS   │
                     └───────────────┬───────────────┘
                                     │ HTTP / JSON
                     ┌───────────────▼───────────────┐
                     │   ServidorHttp (com.sun.net.  │
                     │   httpserver) — capa web/API  │
                     └───┬───────────────────────┬────┘
                         │                       │
            ┌────────────▼───────────┐  ┌────────▼─────────────┐
            │   MotorSimilitud        │  │   MotorClustering     │
            │  (Requerimiento 1)      │  │  (Requerimiento 2)    │
            └──────┬──────────┬───────┘  └──────────┬────────────┘
                   │          │                      │
      ┌────────────▼──┐   ┌───▼───────────┐   ┌──────▼─────────────┐
      │ similitud/     │   │ similitud/ia  │   │ clustering/         │
      │ clasico/       │   │ (embeddings)  │   │ (enlaces + evaluac.)│
      └────────────────┘   └───────────────┘   └─────────────────────┘
                   │                     │
            ┌──────▼─────────────────────▼──────┐
            │   texto/ (Preprocesador, Stemmer)   │
            └──────────────────┬───────────────────┘
                                │
                     ┌──────────▼───────────┐
                     │  datos/               │
                     │  RepositorioArticulos │
                     │  (carga data/articulos.json) │
                     └───────────────────────┘
```

### 2.2 Paquetes del código fuente (`app/src/main/java/co/edu/uniquindio/bibliometria/`)

| Paquete | Responsabilidad |
|---|---|
| `model` | Entidad de dominio `Articulo` (id, título, autores, abstract, tokens). |
| `datos` | `RepositorioArticulos`: carga y cachea el corpus desde `data/articulos.json`. |
| `texto` | `Preprocesador`, `Stemmer`, `ListaStopwords`: pipeline de preprocesamiento de texto. |
| `similitud` | `MotorSimilitud` (orquestador) y `ResultadoSimilitud` (DTO de resultado). |
| `similitud.clasico` | Los 4 algoritmos clásicos + `TfIdfVectorizador`. |
| `similitud.ia` | Los 2 algoritmos con IA + `RepositorioEmbeddings` + `VectorizadorEmbeddings`. |
| `clustering` | Algoritmo aglomerativo genérico, las 3 estrategias de enlace, dendrograma. |
| `clustering.evaluacion` | `CorrelacionCofenetica`: métrica de evaluación interna. |
| `web` | `ServidorHttp`: servidor HTTP, rutas de la API REST, archivos estáticos. |
| `util` | `JsonParser` / `JsonEscritor`: utilidades de lectura/escritura JSON propias. |

### 2.3 Decisión de arquitectura: aplicación Java sin framework externo

El backend se implementó como una aplicación Java 17 **sin dependencias externas**, usando
`com.sun.net.httpserver.HttpServer` (incluido en el JDK) en lugar de Spring Boot, y un parser/escritor JSON
propio en lugar de Jackson/Gson. La razón es práctica (el entorno de desarrollo no tenía acceso a Maven
Central para verificar la compilación con esas dependencias) pero también tiene una ventaja pedagógica: todo
el comportamiento de la aplicación —incluyendo la serialización de datos— es auditable y no depende de
"cajas negras". El `pom.xml` sigue siendo un proyecto Maven estándar y compila igualmente con
`mvn clean package` en cualquier entorno con acceso normal a internet. Esta decisión se declara también en
`docs/DECLARACION_USO_IA.md`.

### 2.4 Diagrama de clases y paquetes

El siguiente diagrama resume las clases principales de cada paquete y sus relaciones de composición
(línea sólida), dependencia de uso (línea punteada gris, "usa") e implementación de interfaz (línea
punteada dorada). `BibliometriaApplication` es el punto de entrada: construye e inyecta manualmente
(sin contenedor de inyección de dependencias) todos los objetos del sistema, siguiendo un estilo de
*composición explícita* que hace visibles y auditables todas las dependencias entre clases.

![Diagrama de clases y paquetes](imagenes/05-diagrama-clases.png)

Los paquetes `similitud.clasico` y `similitud.ia` agrupan, respectivamente, los 4 algoritmos clásicos y
los 2 algoritmos de IA del Requerimiento 1 (sección 4); el paquete `clustering`, junto con su
subpaquete `clustering.evaluacion`, agrupa el algoritmo aglomerativo genérico, las 3 estrategias de
enlace (patrón *Strategy* sobre la interfaz `EstrategiaEnlace`) y la métrica de validación del
Requerimiento 2 (sección 5).

---

## 3. Fuentes de datos y preprocesamiento

### 3.1 Extracción de metadatos (carpeta `extraction/`)

Los 20 PDFs (`recursos/articulos-originales/`) tienen formatos de columnas distintos según la editorial
(Elsevier, Springer, MDPI, Emerald, entre otras), lo que hace que la extracción automática de texto con
`pdftotext` desordene el abstract en varios casos (el texto de columnas adyacentes se intercala). El proceso
seguido fue:

1. Extracción de texto plano de cada PDF (`pdftotext`).
2. Heurística de localización del abstract (entre el encabezado "Abstract"/"A B S T R A C T" y el inicio de la
   sección "1. Introduction" o la lista de "Keywords").
3. **Revisión manual de los 20 resultados** contra el PDF original, con corrección explícita de los casos donde
   la heurística fallaba (columnas intercaladas, referencias bibliográficas insertadas a mitad de un
   párrafo, palabras partidas por guion de fin de línea).
4. Normalización de caracteres especiales (ligaduras tipográficas `ﬁ`, `ﬂ`; comillas tipográficas; guiones
   largos) a su forma ASCII equivalente.

El resultado (`extraction/articulos.json`, copiado como recurso de la aplicación en
`app/src/main/resources/data/articulos.json`) es la única fuente de datos que usa la aplicación en tiempo de
ejecución: no se re-procesan los PDFs en cada arranque, lo cual hace el comportamiento reproducible y evita
tener que empaquetar una librería de extracción de PDF dentro de la aplicación desplegada.

### 3.2 Preprocesamiento de texto (`texto.Preprocesador`)

Se implementaron **dos variantes** del pipeline de preprocesamiento, elegidas según lo que necesita cada
familia de algoritmos:

| Pipeline | Pasos | Usado por |
|---|---|---|
| `normalizarParaEdicion` | minúsculas, remoción de acentos (NFD), remoción de puntuación | Levenshtein |
| `procesarParaVectorial` | tokenizar → quitar stopwords → **stemming** (Porter simplificado) | Needleman-Wunsch (a nivel de tokens), TF-IDF/Coseno, Jaccard |
| `procesarParaEmbeddings` | tokenizar → quitar stopwords (**sin** stemming) | Similitud Coseno / Distancia Euclidiana sobre embeddings |

**Nota de diseño importante:** los algoritmos basados en embeddings usan tokens **sin stemming**, a
diferencia de TF-IDF/Jaccard. La razón es que el modelo de embeddings preentrenado (GloVe) tiene vectores para
palabras reales del idioma inglés ("literacy", "assessment"), pero no para raíces truncadas artificiales que
produce el stemmer ("literaci", "assess"). Aplicar stemming antes de la búsqueda en el vocabulario de
embeddings reduciría drásticamente la cantidad de palabras que logran encontrar su vector (en pruebas
tempranas del proyecto, la cobertura pasó de ~46% a **97%–100%** de las palabras del abstract al corregir este
detalle). Este es exactamente el tipo de matiz algorítmico que el proyecto pide analizar críticamente.

El **stemmer** (`texto.Stemmer`) es una implementación propia y simplificada del algoritmo de Porter (1980):
calcula la "medida" silábica (m) de una palabra a partir de su patrón de vocales/consonantes, y aplica reglas
de sufijos en 5 pasos sucesivos (por ejemplo, `"sses"→"ss"`, `"eed"→"ee"` si m>0, `"ational"→"ate"` si m>0,
etc.). No se usó ninguna librería de stemming (Snowball, Lucene Analyzer, etc.).

Las **stopwords** (`texto.ListaStopwords`) son una lista estática de ~180 palabras vacías del inglés (idioma
de los 20 abstracts), definida manualmente.

---

## 4. Requerimiento 1 — Análisis de Similitud Textual

### 4.1 Algoritmos clásicos

#### 4.1.1 Distancia de Levenshtein

Mide el número mínimo de inserciones, borrados y sustituciones de un carácter para transformar la cadena A en
la cadena B, mediante programación dinámica:

```
D[0][j] = j                                        (insertar j caracteres)
D[i][0] = i                                        (borrar i caracteres)
D[i][j] = D[i-1][j-1]                               si A[i] == B[j]
D[i][j] = 1 + min(D[i-1][j], D[i][j-1], D[i-1][j-1]) en otro caso
```

El resultado se normaliza como `similitud = 1 - distancia / max(|A|, |B|)`. Se aplica sobre el texto completo
normalizado (nivel de carácter), lo que lo hace muy sensible a diferencias de longitud y orden de palabras.

**Implementación:** `similitud.clasico.LevenshteinSimilitud`.

#### 4.1.2 Needleman-Wunsch

Algoritmo clásico de alineamiento global de secuencias (Needleman & Wunsch, 1970). A diferencia de
Levenshtein, aquí se aplica **a nivel de tokens/palabras** ya preprocesadas (tras tokenizar, quitar stopwords y
aplicar stemming), con un esquema de puntuación `match = +2`, `mismatch = -1`, `gap = -1`:

```
F[0][j] = j · gap
F[i][0] = i · gap
F[i][j] = max( F[i-1][j-1] + s(Ai,Bj),  F[i-1][j] + gap,  F[i][j-1] + gap )
```

Esta elección metodológica (carácter vs. token) es intencional: permite comparar en el análisis crítico
(sección 4.4) cómo la granularidad del algoritmo de alineamiento afecta el resultado.

**Implementación:** `similitud.clasico.NeedlemanWunschSimilitud`.

#### 4.1.3 Similitud del Coseno sobre TF-IDF

Cada abstract se representa como un vector disperso donde cada dimensión es un término del corpus, ponderado
por TF-IDF:

```
tf(t,d)  = ocurrencias(t,d) / |d|
idf(t)   = ln( (1+N) / (1+df(t)) ) + 1        (N = 20 documentos, df = documentos que contienen t)
tfidf(t,d) = tf(t,d) · idf(t)

cos(θ) = (A · B) / (||A|| · ||B||)
```

El IDF se calcula **una sola vez sobre los 20 abstracts completos** (no solo sobre el par seleccionado), para
que los pesos reflejen qué tan específico es un término en todo el corpus.

**Implementación:** `similitud.clasico.TfIdfVectorizador` + `CosenoTfIdfSimilitud`.

#### 4.1.4 Coeficiente de Jaccard

Cada abstract se representa como el **conjunto** de tokens distintos que contiene:

```
J(A,B) = |A ∩ B| / |A ∪ B|
```

**Implementación:** `similitud.clasico.JaccardSimilitud`.

### 4.2 Algoritmos basados en Inteligencia Artificial

Se usa un modelo de **embeddings preentrenado GloVe (100 dimensiones, Wikipedia + Gigaword)**, tal como lo
habilita explícitamente el enunciado del proyecto. Por restricciones del entorno de desarrollo (sin acceso a
Maven Central para incluir una librería Java de embeddings), el modelo se descargó una sola vez con la
librería Python `gensim` y se **filtró** para conservar solo las ~1000 palabras que efectivamente aparecen en
el corpus de 20 abstracts, generando un recurso de menos de 1 MB (`embeddings/glove100_filtrado.tsv`) en lugar
de los cientos de MB del modelo completo. Ese proceso de filtrado (`extraction/build_embeddings.py`) es
infraestructura de preparación de datos, no uno de los algoritmos exigidos por el curso.

Cada abstract se representa como el **promedio de los vectores de palabra** ("average word embeddings") de
sus tokens (sin stemming, ver sección 3.2):

```
V(documento) = (1/k) · Σ embedding(palabra_i)     para las k palabras del documento encontradas en GloVe
```

#### 4.2.1 Similitud del Coseno sobre embeddings

```
cos(θ) = (V_A · V_B) / (||V_A|| · ||V_B||)
```

**Implementación:** `similitud.ia.CosenoEmbeddingSimilitud`.

#### 4.2.2 Distancia Euclidiana sobre embeddings

```
dist(A,B) = sqrt( Σ (V_A,i - V_B,i)² )
similitud = 1 / (1 + dist)          (transformación para expresarlo en la misma escala [0,1] que los demás)
```

**Implementación:** `similitud.ia.EuclidianaEmbeddingSimilitud`.

El cálculo de la similitud/distancia sobre los vectores (producto punto, norma, suma de cuadrados) se
implementó manualmente con operaciones aritméticas básicas sobre arreglos `double[]`; no se usó ninguna
librería de álgebra lineal (Apache Commons Math, ND4J, etc.) para ese cálculo.

### 4.3 Caso de estudio (Requerimiento 1)

Se seleccionaron dos abstracts reales del corpus:

- **Artículo 1:** *"Artificial intelligence literacy in higher and adult education: A scoping literature
  review"* (Laupichler et al., 2022) — 97 tokens tras preprocesamiento.
- **Artículo 2:** *"Systematic literature review on opportunities, challenges, and future research
  recommendations of artificial intelligence in education"* (Chiu et al., 2023) — 70 tokens tras
  preprocesamiento.

#### 4.3.1 Ejemplo ilustrativo compacto del llenado de matrices

Antes de mostrar los resultados reales sobre los abstracts completos (varios miles de caracteres, cuya matriz
de programación dinámica sería demasiado grande para incluir en un documento), se ilustra el mecanismo interno
de cada algoritmo de programación dinámica con un ejemplo compacto.

**Levenshtein**, ejemplo clásico de la literatura, `"kitten"` vs `"sitting"`:

| | ∅ | s | i | t | t | i | n | g |
|---|---|---|---|---|---|---|---|---|
| **∅** | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
| **k** | 1 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
| **i** | 2 | 2 | 1 | 2 | 3 | 4 | 5 | 6 |
| **t** | 3 | 3 | 2 | 1 | 2 | 3 | 4 | 5 |
| **t** | 4 | 4 | 3 | 2 | 1 | 2 | 3 | 4 |
| **e** | 5 | 5 | 4 | 3 | 2 | 2 | 3 | 4 |
| **n** | 6 | 6 | 5 | 4 | 3 | 3 | 2 | 3 |

Distancia final `D[6][7] = 3` (sustituir k→s, sustituir e→i, insertar g). Similitud = `1 - 3/max(6,7) = 0.5714`.

**Needleman-Wunsch**, a nivel de tokens preprocesados, comparando dos frases cortas y temáticamente
relacionadas con el artículo 1 ("AI literacy adult education" vs "AI literacy higher education"), con
`match=+2, mismatch=-1, gap=-1`:

Tokens A: `[ai, literaci, adult, educ]` — Tokens B: `[ai, literaci, higher, educ]`

| | ∅ | ai | literaci | higher | educ |
|---|---|---|---|---|---|
| **∅** | 0 | -1 | -2 | -3 | -4 |
| **ai** | -1 | 2 | 1 | 0 | -1 |
| **literaci** | -2 | 1 | 4 | 3 | 2 |
| **adult** | -3 | 0 | 3 | 3 | 2 |
| **educ** | -4 | -1 | 2 | 2 | 5 |

Puntaje final `F[4][4] = 5` (2 coincidencias exactas "ai" y "literaci" = +4, "educ" al final = +2, un
mismatch adult/higher = -1, sobre una diagonal favorable). Similitud normalizada = `0.75`.

#### 4.3.2 Resultados reales de los 6 algoritmos (artículo 1 vs. artículo 2)

Estos valores fueron generados **ejecutando la aplicación real** (`GET /api/similitud?idA=1&idB=2`), no
calculados a mano:

| Algoritmo | Similitud | Detalle relevante |
|---|---|---|
| Levenshtein (clásico) | **0.3206** | distancia = 767 caracteres, sobre longitudes 1129 y 887 |
| Needleman-Wunsch (clásico) | **0.1237** | puntaje de alineamiento = -61, sobre 97 y 70 tokens |
| Coseno TF-IDF (clásico) | **0.1992** | ángulo = 78.5°, producto punto = 0.0183 |
| Jaccard (clásico) | **0.1327** | intersección = 15 palabras, unión = 113 palabras |
| Coseno sobre embeddings (IA) | **0.9614** | ángulo = 15.97°, cobertura 97/97 y 64/70 palabras |
| Distancia Euclidiana sobre embeddings (IA) | **0.5242** | distancia euclidiana = 0.908 |

Palabras en común (Jaccard, tras stemming): `ai, artifici, content, current, educ, futur, identifi, intellig,
learn, literatur, relev, result, technologi, understand, us`.

Términos con mayor peso TF-IDF: en el artículo 1 destacan *adult* (0.104), *literaci* (0.091), *ai* (0.076); en
el artículo 2 destacan *ai* (0.105), *outcom* (0.076), *examin* (0.070) — coherente con que ambos abstracts
tratan IA en educación, pero el primero enfatiza la alfabetización de adultos y el segundo un mapeo sistemático
de oportunidades/desafíos.

### 4.4 Análisis crítico: clásicos vs. IA

El resultado más notable del caso de estudio es la **brecha grande** entre los algoritmos clásicos
(0.12–0.32) y los algoritmos de IA (0.52–0.96) para el **mismo par de artículos**. Esto no es un error: revela
una diferencia fundamental de qué es lo que cada familia de algoritmos realmente mide:

- Los algoritmos clásicos (Levenshtein, Needleman-Wunsch, Jaccard, TF-IDF) miden **coincidencia léxica
  literal**: cuentan caracteres o palabras que aparecen igual (o casi igual) en ambos textos. Dos abstracts
  pueden tratar exactamente el mismo tema con vocabulario distinto (sinónimos, paráfrasis) y aun así obtener
  una similitud clásica baja, como ocurre aquí: solo 15 de 113 palabras distintas son literalmente compartidas.
- Los algoritmos de IA (embeddings) miden **cercanía semántica**: el modelo GloVe fue entrenado sobre un
  corpus masivo de texto y aprendió que palabras usadas en contextos similares (p. ej. "students",
  "teachers", "learning", "assessment") tienen vectores cercanos, aunque los dos abstracts no compartan esas
  palabras exactas. Por eso el coseno sobre embeddings (0.96) refleja mucho mejor la intuición humana de que
  "estos dos artículos sí tratan del mismo tema" (ambos son *revisiones de literatura sobre IA en educación*).
- Entre los dos algoritmos de IA, la **distancia euclidiana** (0.52) es notablemente más conservadora que el
  **coseno** (0.96). Esto es un efecto conocido: el coseno solo mide la *dirección* de los vectores promedio
  (ambos abstracts "apuntan" hacia el mismo vecindario semántico de "IA + educación"), mientras que la
  distancia euclidiana también penaliza diferencias en la *magnitud* del vector promedio, la cual depende de
  cuántas palabras específicas y poco frecuentes (con vectores de mayor norma) contiene cada abstract.
- Dentro de los algoritmos clásicos, Levenshtein (0.32) es mayor que Needleman-Wunsch (0.12) para este par,
  lo cual ilustra el efecto de la granularidad: a nivel de carácter, dos textos largos en inglés comparten
  muchísimos fragmentos cortos (espacios, terminaciones "-tion", "-ing", artículos "the", "of") aunque traten
  temas distintos, inflando la similitud de Levenshtein; a nivel de token completo, esas coincidencias
  parciales no cuentan, y el esquema de penalización por *gap* (palabras sin pareja) es más severo.

**Conclusión del caso de estudio:** para esta tarea (encontrar artículos temáticamente relacionados dentro de
una revisión bibliográfica), los algoritmos de IA capturan mejor la similitud "real" que percibiría un
investigador humano, mientras que los algoritmos clásicos son más útiles para detectar **coincidencia textual
literal** (por ejemplo, para detectar plagio o abstracts casi duplicados), que es un problema distinto.

### 4.5 Análisis formal de complejidad computacional (Requerimiento 1)

Siguiendo la metodología vista en el curso —conteo de operaciones elementales, tabla del número de
iteraciones de cada ciclo, derivación de T(n) y reducción a notación O mediante la regla de la suma para
bloques secuenciales, la regla de la constante multiplicativa y la regla de los términos de orden
inferior— se presenta el análisis de **peor caso** de los 6 algoritmos de similitud. Salvo que se indique
lo contrario, n = |A| y m = |B| son la longitud (en caracteres o en tokens, según el algoritmo) de los dos
abstracts comparados.

#### 4.5.1 Distancia de Levenshtein — O(n·m)

`LevenshteinSimilitud.construirMatriz` construye una matriz D de (n+1) filas por (m+1) columnas. El ciclo
externo recorre las filas y, por cada fila, un ciclo interno recorre las columnas; dentro del ciclo interno
se ejecuta un número constante c de operaciones elementales (una comparación de caracteres, una asignación,
y en el peor caso tres comparaciones y una suma para obtener el mínimo):

| Ciclo | Nº de iteraciones |
|---|---|
| Externo (fila i) | n+1 |
| Interno (columna j), por cada i | m+1 |

T(n,m) = (n+1)·(m+1)·c + O(n) + O(m) = c·n·m + c·n + c·m + c + n + m + 1

Descartando los términos de orden inferior (n, m, constantes) frente al término dominante n·m, y aplicando
la regla de la constante multiplicativa: **T(n,m) ∈ O(n·m)**, el mismo orden reportado en la literatura
clásica de programación dinámica para distancia de edición (Wagner & Fischer, 1974).

#### 4.5.2 Needleman-Wunsch — O(n·m)

`NeedlemanWunschSimilitud.construirMatriz` tiene la misma estructura de doble ciclo anidado que Levenshtein
sobre una matriz F de (n+1)×(m+1); solo cambia la función de puntuación por match/mismatch/gap, que es una
comparación más una asignación (costo constante). El conteo de operaciones elementales es análogo al de
4.5.1: **T(n,m) ∈ O(n·m)**, donde aquí n y m son el número de **tokens** (no caracteres) de cada abstract
tras el preprocesamiento (tokenizar + quitar stopwords + stemming).

#### 4.5.3 Similitud del Coseno sobre TF-IDF — O(N·V) una vez + O(d) por comparación

Este algoritmo tiene dos fases con costos distintos:

- **Construcción del IDF** (`TfIdfVectorizador`, calculada una sola vez sobre todo el corpus, sección 3.1):
  un ciclo sobre los N=20 documentos y, por cada uno, un ciclo sobre los términos distintos del documento,
  para acumular en cuántos documentos aparece cada término. T(N,V) = N·V·c ⇒ **O(N·V)**, donde V es el
  tamaño del vocabulario del corpus.
- **Cálculo del coseno entre un par de abstracts** (`CosenoTfIdfSimilitud.calcular`): un único ciclo de d
  iteraciones sobre el vocabulario combinado de los dos vectores dispersos, acumulando en la misma
  iteración el producto punto y las dos normas. T(d) = d·c ⇒ **O(d)**, con d ≤ V.

Como el IDF se calcula una única vez y se reutiliza en todas las comparaciones, el costo que efectivamente
se paga **por cada comparación de dos artículos** es O(d), lineal en el vocabulario combinado.

#### 4.5.4 Coeficiente de Jaccard — O(n+m)

`JaccardSimilitud.calcular` inserta los n tokens de A y los m tokens de B en dos conjuntos (`HashSet`, con
costo amortizado O(1) por inserción mediante tabla hash), y luego recorre el conjunto más pequeño para
contar la intersección. Son tres ciclos simples, no anidados entre sí:

T(n,m) = n·c₁ (llenar conjunto A) + m·c₂ (llenar conjunto B) + min(n,m)·c₃ (intersección) + (n+m)·c₄ (unión)

Por la regla de la suma de bloques secuenciales y la regla de las constantes: **T(n,m) ∈ O(n+m)**, lineal
en el tamaño combinado de los dos documentos — el de menor orden de los 6 algoritmos.

#### 4.5.5 Similitud del Coseno y Distancia Euclidiana sobre embeddings — O(k)

Los dos algoritmos de IA comparten la misma estructura de costo, dominada por
`VectorizadorEmbeddings.vectorizar`: un ciclo sobre los k tokens del documento, con una búsqueda en un
`HashMap` (O(1) amortizado) y, si la palabra tiene vector, una suma componente a componente sobre un vector
de **dimensión fija d=100** (constante del modelo GloVe, no una variable de la entrada):

T(k) = k·c₁ + k·d·c₂

Como d es una constante, k·d·c₂ es k·(constante)·c₂, que por la regla de la constante multiplicativa tiene
el mismo orden que k. El paso final de cada algoritmo (`CosenoEmbeddingSimilitud`/
`EuclidianaEmbeddingSimilitud.calcular`) recorre una sola vez los d=100 componentes de los dos vectores
promedio ya calculados: O(d) = O(1) por ser d constante. Por la regla de la suma:
**T(k) ∈ O(k)**, lineal en el número de tokens del documento, con una constante multiplicativa mayor que la
de Jaccard (búsqueda en mapa + suma vectorial de 100 componentes por token, en vez de una simple inserción
en un conjunto).

#### 4.5.6 Resumen comparativo

| Algoritmo | T(n) dominante | Orden | Variable dominante |
|---|---|---|---|
| Levenshtein | c·n·m | O(n·m) | n,m = caracteres |
| Needleman-Wunsch | c·n·m | O(n·m) | n,m = tokens |
| Coseno TF-IDF (por comparación) | c·d | O(d) | d = vocabulario combinado |
| Jaccard | c·(n+m) | O(n+m) | n,m = tokens |
| Coseno sobre embeddings | c·k | O(k) | k = tokens (d=100 es constante) |
| Distancia Euclidiana sobre embeddings | c·k | O(k) | k = tokens (d=100 es constante) |

Los dos algoritmos de programación dinámica (Levenshtein, Needleman-Wunsch) son, en notación asintótica,
los más costosos (cuadráticos, O(n·m)) frente a los cuatro restantes (lineales). Sobre los 20 abstracts del
corpus (cientos de caracteres, decenas de tokens cada uno) esta diferencia es imperceptible en tiempo de
ejecución real, pero sería determinante si el corpus creciera a miles de documentos largos: es un ejemplo
concreto de por qué el análisis asintótico importa incluso cuando, para una entrada pequeña, todos los
algoritmos "se sienten igual de rápidos".

---

## 5. Requerimiento 2 — Agrupamiento Jerárquico

### 5.1 Preprocesamiento y matriz de distancias

Se reutiliza el mismo pipeline de TF-IDF del Requerimiento 1 sobre los 20 abstracts, y se construye una matriz
de distancias 20×20 con `distancia = 1 - similitud_coseno(TF-IDF)` (`clustering.DistanciaCorpus`). Se eligió
esta métrica como punto de partida porque ya estaba validada en el Requerimiento 1 y produce una noción de
distancia clásica y determinística (no dependiente de una red neuronal), apropiada como línea base para el
clustering.

### 5.2 Algoritmo aglomerativo genérico (fórmulas de Lance-Williams)

Se implementó un único algoritmo aglomerativo (`clustering.ClusteringJerarquico`) que parte de 20 clusteres
unitarios y, en cada iteración, fusiona el par con menor distancia, actualizando las distancias del nuevo
cluster hacia los demás mediante la fórmula de Lance-Williams correspondiente a cada criterio de enlace
(estrategia `EstrategiaEnlace`, patrón *Strategy*):

| Enlace | Fórmula de actualización | Clase |
|---|---|---|
| **Single Linkage** | `d(I∪J, M) = min(d(I,M), d(J,M))` | `EnlaceSimple` |
| **Complete Linkage** | `d(I∪J, M) = max(d(I,M), d(J,M))` | `EnlaceCompleto` |
| **Average Linkage (UPGMA)** | `d(I∪J, M) = (\|I\|·d(I,M) + \|J\|·d(J,M)) / (\|I\|+\|J\|)` | `EnlacePromedio` |

El resultado de cada ejecución es un **dendrograma** (`NodoDendrograma`): árbol binario donde cada hoja es un
artículo y cada nodo interno registra la altura (distancia) a la que se fusionaron sus dos subárboles.

### 5.3 Visualización (dendrogramas)

La aplicación web dibuja los 3 dendrogramas con SVG generado dinámicamente en el navegador
(`app.js::construirSvgDendrograma`), a partir del árbol JSON devuelto por `GET /api/clustering`. Cada hoja se
etiqueta con el id y el título abreviado del artículo.

![Vista del corpus](imagenes/01-corpus.png)
![Comparación de similitud (Requerimiento 1)](imagenes/02-similitud.png)
![Dendrograma — vista inicial](imagenes/03-clustering.png)
![Dendrograma — Average Linkage (el más coherente)](imagenes/04-clustering-average.png)

### 5.4 Evaluación interna: Coeficiente de Correlación Cofenética

Para determinar cuál de los 3 algoritmos produce el agrupamiento más coherente, se implementó el
**Coeficiente de Correlación Cofenética** (Sokal & Rohlf, 1962):

1. La **distancia cofenética** entre dos artículos i, j es la altura a la que el dendrograma los fusiona por
   primera vez en un mismo cluster (`CorrelacionCofenetica.calcularMatrizCofenetica`, recorrido recursivo del
   árbol).
2. La **correlación cofenética** es el coeficiente de correlación de Pearson (implementado manualmente, sin
   Apache Commons Math) entre las distancias originales (matriz TF-IDF) y las distancias cofenéticas, tomando
   el triángulo superior de ambas matrices (190 pares para 20 artículos).

Un valor cercano a 1 indica que el dendrograma preserva fielmente las distancias originales.

**Resultados reales** (`GET /api/clustering`, ejecutado sobre los 20 abstracts):

| Algoritmo de enlace | Correlación cofenética |
|---|---|
| Single Linkage | 0.7487 |
| Complete Linkage | 0.5717 |
| **Average Linkage (UPGMA)** | **0.8021** ← más coherente |

**Interpretación:** Average Linkage produce el agrupamiento más coherente con las distancias TF-IDF
originales. Esto es consistente con la teoría: Single Linkage es propenso al "efecto de encadenamiento"
(*chaining*) — tiende a ir anexando artículos uno por uno a un cluster grande en lugar de formar grupos
temáticos claros, lo que distorsiona las distancias cofenéticas para los primeros artículos en encadenarse.
Complete Linkage, en el extremo opuesto, fuerza clusters muy compactos y homogéneos en tamaño, lo cual también
distorsiona las distancias cuando el corpus no tiene una estructura de clusters perfectamente balanceada — de
hecho obtuvo aquí la correlación más baja. Average Linkage, al promediar (ponderado por tamaño) las
distancias entre todos los pares de artículos de los dos clusters que se fusionan, es un balance intermedio
que en la práctica suele ajustarse mejor a corpus de texto con estructura temática difusa, como es el caso de
20 revisiones de literatura sobre un dominio relacionado (IA en educación) donde los límites entre subtemas
(alfabetización en IA, evaluación educativa, aprendizaje colaborativo, ChatGPT, etc.) no son tajantes.

### 5.5 Análisis formal de complejidad computacional (Requerimiento 2)

Se aplica la misma metodología de la sección 4.5. Sea n=20 el número de artículos del corpus (clusteres
iniciales / hojas del dendrograma).

#### 5.5.1 Algoritmo aglomerativo genérico — O(n³)

`ClusteringJerarquico.ejecutar` realiza exactamente n-1 iteraciones (fusiones) hasta reducir los n
clusteres iniciales a 1 solo. En la iteración k (k=1,…,n-1) hay (n-k+1) clusteres activos, y cada iteración
ejecuta dos pasos:

| Paso | Costo en la iteración k |
|---|---|
| 1. Buscar el par de clusteres con menor distancia (recorrido de la matriz activa) | O((n-k+1)²) |
| 2. Actualizar distancias del nuevo cluster con Lance-Williams (`EstrategiaEnlace.calcular`, O(1) por par) | O(n-k+1) |

Por la regla de los términos de orden inferior, el costo de la iteración k es O((n-k+1)²) + O(n-k+1) =
O((n-k+1)²). Sumando sobre las n-1 iteraciones (regla de la suma):

T(n) = Σ_{k=1}^{n-1} (n-k+1)² = Σ_{j=2}^{n} j² = [n(n+1)(2n+1)/6] − 1 ≈ n³/3 + términos de orden inferior

(usando la identidad estándar Σ_{j=1}^{n} j² = n(n+1)(2n+1)/6). Descartando los términos de orden inferior
(n², n) y la constante 1/3: **T(n) ∈ O(n³)**, el orden de complejidad estándar del clustering jerárquico
aglomerativo implementado de forma directa (sin las estructuras de datos especializadas de algoritmos como
SLINK/CLINK, que logran O(n²) únicamente para enlace simple/completo, no para enlace promedio genérico).

Este análisis es **independiente del criterio de enlace usado**: `EnlaceSimple`, `EnlaceCompleto` y
`EnlacePromedio` ejecutan la misma fórmula de Lance-Williams en tiempo O(1) por par de clusteres (una
comparación `Math.min`/`Math.max`, o una suma ponderada con dos multiplicaciones y una división); cambiar
de estrategia (patrón *Strategy*) cambia únicamente la constante multiplicativa c, no el orden O(n³) del
algoritmo genérico.

#### 5.5.2 Construcción de la matriz de distancias inicial — O(n²·d)

Antes de iniciar el clustering, `DistanciaCorpus` calcula la distancia TF-IDF entre cada uno de los
C(n,2) = n(n-1)/2 pares de artículos; cada cálculo de similitud coseno cuesta O(d) (sección 4.5.3, con d =
tamaño del vocabulario combinado del par). T(n) = [n(n-1)/2]·O(d) ⇒ **O(n²·d)**. Para n=20 y d del orden de
cientos de términos, este paso es, en la práctica, más costoso que las 19 iteraciones del algoritmo
aglomerativo en sí, aunque ambos son de complejidad polinomial.

#### 5.5.3 Correlación Cofenética — O(n²)

`CorrelacionCofenetica.calcularMatrizCofenetica` obtiene, para cada nodo interno del dendrograma, el
conjunto de hojas de su subárbol izquierdo y derecho mediante un único recorrido post-orden del árbol
(O(n) nodos internos), y registra la altura de fusión de ese nodo para cada par (hoja izquierda, hoja
derecha) de sus dos subárboles; cada uno de los C(n,2) pares de hojas se visita exactamente una vez a lo
largo de todo el recorrido, dando T(n) = O(n²). Sobre las dos matrices resultantes (distancias originales
y cofenéticas, ambas de n(n-1)/2 pares), `calcularCorrelacion` recorre los pares dos veces de forma lineal
(una para las medias, otra para las sumas de productos y cuadrados del coeficiente de Pearson): dos ciclos
de tamaño n(n-1)/2 ⇒ O(n²). Por la regla de la suma: **T(n) ∈ O(n²)**.

#### 5.5.4 Resumen comparativo

| Etapa | T(n) dominante | Orden |
|---|---|---|
| Matriz de distancias inicial (TF-IDF) | c·n²·d | O(n²·d) |
| Algoritmo aglomerativo (Lance-Williams) | c·n³/3 | O(n³) |
| Correlación cofenética | c·n² | O(n²) |

El algoritmo aglomerativo genérico es la etapa asintóticamente más costosa, O(n³), lo cual coincide con lo
reportado en la literatura: el clustering jerárquico aglomerativo "directo" (sin las optimizaciones
específicas de SLINK/CLINK) escala mal a corpus grandes. Para el corpus de n=20 artículos del proyecto,
n³=8000 comparaciones es un costo trivial en tiempo real (milisegundos); pero el análisis formal es
precisamente lo que permite predecir que, por ejemplo, agrupar 2000 artículos (100 veces más) tomaría del
orden de 100³ = 1 000 000 de veces más operaciones — no simplemente 100 veces más — lo cual justifica por
qué, para corpus muy grandes, se preferirían algoritmos de clustering con mejor complejidad asintótica
(p. ej. k-means, O(n·k) por iteración).

---

## 6. Aplicación web

La interfaz web (`app/src/main/resources/static/`) es una aplicación de una sola página (HTML + CSS + JS
vainilla, sin frameworks ni CDN) con 4 secciones:

1. **Corpus:** tabla de los 20 artículos con acceso al abstract completo.
2. **Requerimiento 1:** selección dinámica de dos artículos (tal como exige el enunciado) y comparación con
   los 6 algoritmos, mostrando tanto el puntaje como el detalle matemático del cálculo (matriz, ángulo,
   términos con mayor peso, palabras en común, según el algoritmo).
3. **Requerimiento 2:** tabla comparativa de correlación cofenética de los 3 algoritmos, con el más coherente
   resaltado, y visualización interactiva de cada dendrograma.
4. **Acerca del proyecto:** resumen de la solución y enlaces al repositorio.

La API REST que consume el frontend:

| Endpoint | Descripción |
|---|---|
| `GET /api/articulos` | Lista los 20 artículos (metadatos + abstract). |
| `GET /api/articulos/{id}` | Un artículo específico. |
| `GET /api/similitud?idA=&idB=` | Ejecuta los 6 algoritmos de similitud sobre el par indicado. |
| `GET /api/clustering` | Ejecuta los 3 algoritmos de clustering y su evaluación cofenética. |

---

## 7. Pruebas de verificación

Se implementó una batería de pruebas manuales (`app/src/test/java/.../PruebasAlgoritmos.java`, sin JUnit por
la misma razón declarada en la sección 2.3) que compara la salida de cada algoritmo contra casos de referencia
matemáticamente conocidos: distancia de Levenshtein("kitten","sitting")=3, similitud de un texto consigo mismo
= 1.0 en los 4 algoritmos aplicables, Jaccard = 0 para conjuntos disjuntos, y una prueba de clustering sobre 4
puntos sintéticos con estructura de 2 pares claramente separados, verificando que el algoritmo los fusiona en
el orden correcto y que la correlación cofenética resultante es alta (>0.9). Las 17 aserciones pasan
(ver `README.md` para el comando de ejecución).

---

## 8. Despliegue

La aplicación se empaqueta como una imagen Docker (`app/Dockerfile`, build multi-etapa: Maven + JDK para
compilar, JRE liviano para ejecutar) y puede desplegarse en cualquier plataforma que soporte contenedores
(Render, Railway, Fly.io, un VPS propio, etc.). Instrucciones detalladas paso a paso en el `README.md` del
repositorio y en el propio `Dockerfile` (comentado).

Resumen rápido para Render.com:
1. Conectar el repositorio de GitHub (rama `main`).
2. Crear un "Web Service" nuevo, entorno Docker, root directory `app/`.
3. Render detecta el `Dockerfile` automáticamente y expone el puerto vía la variable de entorno `PORT`
   (ya soportada por `BibliometriaApplication`).

---

## 9. Limitaciones y trabajo futuro

- La extracción de metadatos de los PDFs es semi-automática (requirió corrección manual); un trabajo futuro
  podría mejorar la heurística de segmentación de columnas.
- El modelo de embeddings (GloVe 100d) es relativamente simple comparado con modelos de lenguaje modernos
  (BERT, embeddings de LLMs vía API); se eligió por ser reproducible offline y no depender de una API externa
  de pago para el despliegue.
- La arquitectura modular (interfaces `EstrategiaEnlace`, clases independientes por algoritmo) está preparada
  para incorporar requerimientos adicionales (por ejemplo, búsqueda por autor/título, mencionada en el
  enunciado como posible extensión futura) sin modificar el código existente.

---

## 10. Referencias

- Levenshtein, V. I. (1966). Binary codes capable of correcting deletions, insertions, and reversals.
- Needleman, S. B., & Wunsch, C. D. (1970). A general method applicable to the search for similarities in the
  amino acid sequence of two proteins. *Journal of Molecular Biology*.
- Porter, M. F. (1980). An algorithm for suffix stripping. *Program*, 14(3), 130-137.
- Salton, G., & McGill, M. J. (1983). *Introduction to Modern Information Retrieval* (TF-IDF).
- Jaccard, P. (1912). The distribution of the flora in the alpine zone.
- Lance, G. N., & Williams, W. T. (1967). A general theory of classificatory sorting strategies.
- Sokal, R. R., & Rohlf, F. J. (1962). The comparison of dendrograms by objective methods. *Taxon*, 11(2).
- Pennington, J., Socher, R., & Manning, C. D. (2014). GloVe: Global Vectors for Word Representation.
- Los 20 artículos científicos del corpus (ver `recursos/articulos-originales/` y las referencias completas
  embebidas como metadato `autores`/`titulo` en `extraction/articulos.json`).
