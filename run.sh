#!/usr/bin/env bash
# MiniLPP - démarrage sous Linux / WSL
#
#   ./run.sh            base + compilation + tests + API
#   ./run.sh tests      compilation et tests seulement (aucune base requise)
#
# Le `set -euo pipefail` est la première ligne de tout script Bash sérieux :
# arrêt à la première erreur, variable non définie interdite, échec propagé
# à travers les pipes.
set -euo pipefail
cd "$(dirname "$0")"

VERSION_PILOTE="3.5.10"
PILOTE="lib/mariadb-java-client-${VERSION_PILOTE}.jar"
URL_PILOTE="https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/${VERSION_PILOTE}/mariadb-java-client-${VERSION_PILOTE}.jar"

if [[ ! -f "$PILOTE" ]]; then
  echo "Téléchargement du pilote JDBC MariaDB ${VERSION_PILOTE} (~700 Ko) depuis Maven Central..."
  mkdir -p lib
  curl -fsSL "$URL_PILOTE" -o "$PILOTE"
fi

echo "Compilation..."
mkdir -p out
javac -encoding UTF-8 -d out $(find src test -name '*.java')

echo
echo "Tests :"
java -Dfile.encoding=UTF-8 -cp out ch.minilpp.Tests

if [[ "${1:-}" == "tests" ]]; then
  exit 0
fi

echo
echo "Démarrage de MariaDB (Docker)..."
docker compose up -d

echo "Attente de la disponibilité de la base..."
for _ in $(seq 1 40); do
  if [[ "$(docker inspect --format '{{.State.Health.Status}}' minilpp-db 2>/dev/null)" == "healthy" ]]; then
    break
  fi
  sleep 2
done

echo
echo "Démarrage de l'API sur http://localhost:8080 (Ctrl+C pour arrêter)"
echo
java -Dfile.encoding=UTF-8 -cp "out:$PILOTE" ch.minilpp.Api
