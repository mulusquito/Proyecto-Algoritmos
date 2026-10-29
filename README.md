# Análisis de Algoritmos en el Contexto de la Bibliometría

Proyecto de curso — **Universidad del Quindío**, Programa de Ingeniería de Sistemas y Computación.

Implementación, **desde cero** (sin funciones de alto nivel de similitud/clustering), de algoritmos de análisis
textual sobre un corpus de 20 artículos científicos sobre Inteligencia Artificial en Educación, con enfoque en:

- **Requerimiento 1 — Similitud textual**: 4 algoritmos clásicos (Levenshtein, Needleman-Wunsch, Similitud del
  Coseno con TF-IDF, Coeficiente de Jaccard) + 2 algoritmos basados en Inteligencia Artificial, uno por cada
  modelo de *embeddings* preentrenado (GloVe 100d y FastText 300d), comparados con similitud del coseno y distancia
  euclidiana. Permite seleccionar dos o más artículos.
- **Requerimiento 2 — Agrupamiento jerárquico**: 3 algoritmos de clustering (Single, Complete y Average Linkage)
  construidos desde cero, con dendrogramas y evaluación mediante el Coeficiente de Correlación Cofenética.
- **Requerimiento 3 — Búsqueda de patrones**: coincidencia exacta de cadenas en los 20 abstracts con
  Knuth-Morris-Pratt, Boyer-Moore (carácter malo y sufijo bueno) y Rabin-Karp (rolling hash), sin `indexOf`/`find`,
  con medición separada de los tiempos de preprocesamiento y de búsqueda y traza paso a paso.
- **Seguimiento — Ranking de términos**: los términos más frecuentes de los 20 abstracts, ordenados con 3
  algoritmos no basados en comparaciones (Counting Sort, Radix Sort y Bucket Sort), con medición de tiempos en
  milisegundos y validación del top 15.
- **Aplicación web desplegable** que permite explorar el corpus, comparar artículos, visualizar los
  dendrogramas y buscar patrones de texto interactivamente.

## Estructura del repositorio

```
├── app/                    Aplicación Java (backend + frontend embebido)
│   ├── src/main/java/...   Código fuente (algoritmos, API REST, modelo)
│   ├── src/main/resources/ Datos curados (data/articulos.json), embeddings, frontend estático
│   ├── src/test/java/...   Pruebas de verificación de los algoritmos
│   ├── pom.xml             Proyecto Maven
│   └── Dockerfile          Imagen para despliegue
├── extraction/              Scripts usados para extraer y curar los abstracts/metadatos de los 20 PDFs
├── recursos/articulos-originales/   Los 20 PDFs originales suministrados por el docente
├── docs/
│   ├── imagenes/                    Diagrama de clases y dendrogramas (generados con el código)
│   └── enunciado/Proyecto Algoritmos.pdf
└── README.md
```

## Por qué no hay dependencias externas (sin Spring, sin Jackson)

El backend (`app/`) es una aplicación **Java 17 sin dependencias externas**: usa `com.sun.net.httpserver`
(incluido en el JDK) para la API REST y un parser/escritor JSON propio (`co.edu.uniquindio.bibliometria.util`).
Esta decisión se tomó porque el entorno donde se desarrolló el proyecto no tenía acceso de red a Maven Central
para descargar un framework como Spring Boot. El resultado es, de hecho, una ventaja para un proyecto académico
centrado en la transparencia algorítmica: compila y corre únicamente con el JDK, sin artefactos de "caja negra".
El `pom.xml` sigue siendo un proyecto Maven estándar (para quien prefiera compilar/empaquetar con Maven en un
entorno con acceso a internet normal).

## Cómo ejecutar

### Opción A — Con Maven (recomendado si tienes acceso normal a internet)
```bash
cd app
mvn clean package
java -jar target/bibliometria-algoritmos.jar
```

### Opción B — Solo con el JDK (sin Maven)
```bash
cd app
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d target/classes @sources.txt
cp -r src/main/resources/* target/classes/
java -cp target/classes co.edu.uniquindio.bibliometria.BibliometriaApplication
```

Luego abre **http://localhost:8080** en el navegador.

### Ejecutar las pruebas de verificación
```bash
cd app
find src/main/java src/test/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d target/test-classes @sources.txt
java -cp target/test-classes co.edu.uniquindio.bibliometria.PruebasAlgoritmos
```

## Despliegue

La aplicación se empaqueta con `app/Dockerfile` (compilación con Maven y ejecución con un JRE liviano) y puede
desplegarse en cualquier plataforma que soporte contenedores (Render, Railway, Fly.io, etc.). En Render: crear un
"Web Service" con entorno Docker y directorio raíz `app/`; el puerto se toma de la variable de entorno `PORT`.

## Flujo de ramas de Git

| Rama | Propósito |
|---|---|
| `main` | Rama de **despliegue**: siempre refleja la versión estable que está (o puede ser) desplegada. |
| `develop` | Rama de **desarrollo**: donde se integran los cambios en curso antes de promoverlos a `main`. |

Flujo de trabajo sugerido: crear ramas de feature a partir de `develop` (`feature/nombre`), abrir Pull Request
hacia `develop`, y cuando el trabajo esté listo para producción, fusionar `develop` en `main`.

## Uso de Inteligencia Artificial generativa

Este proyecto usó herramientas de IA generativa como apoyo durante el desarrollo. La declaración explícita de ese
uso, exigida por el enunciado del proyecto, se entrega junto con la documentación del curso.
