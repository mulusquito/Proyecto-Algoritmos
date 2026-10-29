# --- Etapa 1: compilacion -------------------------------------------------
# Se compila con Maven + JDK 17. El proyecto no tiene dependencias externas
# (ver README.md), asi que esta etapa solo
# necesita descargar los plugins basicos de Maven (compiler, jar) la primera
# vez que se construye la imagen en un entorno con acceso normal a internet.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Etapa 2: imagen de ejecucion ------------------------------------------
# Solo se necesita un JRE (no el JDK completo) para ejecutar el jar ya
# compilado, lo que produce una imagen final mas liviana.
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=build /build/target/bibliometria-algoritmos.jar app.jar

# Render/Railway inyectan la variable de entorno PORT en tiempo de
# ejecucion; BibliometriaApplication.main() ya la lee (con 8080 como
# valor por defecto para ejecucion local).
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
