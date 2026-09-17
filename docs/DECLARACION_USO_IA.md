# Declaración de Uso de Inteligencia Artificial Generativa

Conforme a lo exigido en la descripción del proyecto ("El uso de herramientas de inteligencia artificial
generativa como apoyo al desarrollo del proyecto deberá ser declarado explícitamente"), se documenta a
continuación el alcance del uso de IA en este trabajo.

## Herramienta utilizada

**Claude** (Anthropic), a través de la interfaz Claude en modo agente (Cowork), fue usado como asistente de
desarrollo durante la construcción de este proyecto.

## Qué se usó como apoyo (permitido)

- **Extracción y curación de datos**: apoyo para escribir los scripts de extracción de texto de los 20 PDFs
  (`extraction/`), y para revisar manualmente que los abstracts, títulos y autores extraídos fueran correctos
  (varios abstracts requirieron corrección manual porque el formato de columnas de las revistas originales
  producía texto desordenado al extraerlo automáticamente).
- **Andamiaje de la aplicación**: estructura del proyecto Java (paquetes, clases DTO, servidor HTTP, capa web),
  hoja de estilos y JavaScript del frontend (interfaz de usuario, renderizado de dendrogramas en SVG). Esta capa
  es infraestructura de la aplicación, **no** los algoritmos de similitud/clustering exigidos por el curso.
- **Documentación técnica**: redacción y organización de este conjunto de documentos, a partir de los
  resultados reales producidos por la aplicación (los números, matrices y tablas que aparecen en el documento
  técnico fueron generados ejecutando el código, no inventados).
- **Selección de un modelo de embeddings preentrenado** (GloVe 100 dimensiones, entrenado sobre
  Wikipedia + Gigaword) para los dos algoritmos de similitud basados en IA del Requerimiento 1, tal como
  explícitamente lo habilita el enunciado del proyecto ("Los estudiantes pueden utilizar modelos preentrenados
  [...] para vectorizar los abstracts").
- **Redacción de pruebas de verificación** (`PruebasAlgoritmos.java`) usando casos de referencia matemáticos
  conocidos (por ejemplo, la distancia de Levenshtein entre "kitten" y "sitting" = 3, un ejemplo estándar de la
  literatura) para confirmar que cada implementación es correcta.

## Qué NO se hizo con IA (el núcleo algorítmico exigido por el curso)

Los **6 algoritmos de similitud** (Levenshtein, Needleman-Wunsch, Similitud del Coseno sobre TF-IDF,
Coeficiente de Jaccard, Similitud del Coseno sobre embeddings, Distancia Euclidiana sobre embeddings), el
**preprocesamiento de texto** (tokenización, stopwords, stemming tipo Porter) y los **3 algoritmos de
clustering jerárquico** (Single/Complete/Average Linkage) junto con la **métrica de evaluación interna**
(Correlación Cofenética) fueron **implementados explícitamente con estructuras básicas del lenguaje**
(arreglos, matrices, mapas, recursión), sin usar ninguna función de alto nivel de una librería que calcule
directamente similitud, distancia, TF-IDF o clustering (no se usó `scikit-learn`, `nltk`, `Apache Commons Math`,
`Weka`, ni equivalentes). El código de cada algoritmo incluye comentarios Javadoc explicando la fórmula
matemática y la recurrencia de programación dinámica correspondiente, precisamente para que el funcionamiento
interno sea auditable.

La IA generativa se usó como **apoyo de productividad** (escribir código repetitivo de infraestructura,
redactar documentación, sugerir la estructura de las pruebas), nunca para reemplazar el diseño algorítmico ni
el análisis matemático exigido por el curso, en línea con la restricción explícita del enunciado: *"Dichas
herramientas podrán utilizarse como soporte, pero no podrán reemplazar el diseño algorítmico ni el análisis
formal solicitado en el curso."*

## Decisión técnica documentada: aplicación sin framework (Spring Boot)

Una decisión de ingeniería relevante que vale la pena declarar: el entorno de desarrollo usado durante la
construcción de este proyecto no tenía acceso de red a Maven Central, por lo que no fue posible descargar
Spring Boot ni ninguna librería externa para verificar la compilación. En consecuencia, el backend se
reimplementó como una aplicación Java estándar sin dependencias externas (`com.sun.net.httpserver` + un
parser/escritor JSON propio), lo cual permitió compilar y ejecutar el proyecto completo, y correr las pruebas
de verificación, dentro del mismo entorno de desarrollo. El `pom.xml` incluido sigue siendo un proyecto Maven
válido para quien quiera compilarlo con Maven en un entorno con acceso normal a internet.
