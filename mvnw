#!/usr/bin/env sh
set -eu
MAVEN_VERSION=3.9.11
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MAVEN_HOME="$BASE/.mvn/apache-maven-$MAVEN_VERSION"
if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  ARCHIVE="${TMPDIR:-/tmp}/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  echo "Maven nao encontrado. Baixando Apache Maven $MAVEN_VERSION..."
  curl -fL "$URL" -o "$ARCHIVE"
  mkdir -p "$BASE/.mvn"
  tar -xzf "$ARCHIVE" -C "$BASE/.mvn"
fi
exec "$MAVEN_HOME/bin/mvn" "$@"
