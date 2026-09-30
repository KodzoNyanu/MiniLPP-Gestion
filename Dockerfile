# MiniLPP - image pour Cloud Run (ou tout hébergeur de conteneurs).
# Compilation avec javac (pas de Maven/Gradle dans ce projet), puis exécution
# avec le pilote JDBC PostgreSQL déjà utilisé par run.sh/run.ps1 en local.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

ARG PILOTE_VERSION=42.7.13
ENV PILOTE=lib/postgresql-jdbc.jar
COPY src ./src

RUN mkdir -p lib out \
    && curl -fsSL "https://repo1.maven.org/maven2/org/postgresql/postgresql/${PILOTE_VERSION}/postgresql-${PILOTE_VERSION}.jar" \
       -o "$PILOTE" \
    && javac -encoding UTF-8 -d out $(find src -name '*.java')

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY --from=build /app/lib ./lib
COPY web ./web

# Cloud Run route le trafic vers le port exposé ci-dessous, quelle que soit
# la valeur de $PORT ; MINILPP_PORT reste réglable si besoin.
ENV MINILPP_PORT=8080
EXPOSE 8080

CMD ["java", "-Dfile.encoding=UTF-8", "-cp", "out:lib/postgresql-jdbc.jar", "ch.minilpp.Api"]
