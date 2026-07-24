#!/usr/bin/env bash
# IB Trader — cross-platform task runner (Git Bash / Linux / macOS)
# Mirrors Makefile targets. Usage: ./make build

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

if [[ "${OS:-}" == "Windows_NT" ]]; then
  MVN="$ROOT/mvnw.cmd"
else
  MVN="$ROOT/mvnw"
fi

JAR="$ROOT/bootstrap/target/ib-trader.jar"
TARGET="${1:-help}"

show_help() {
  echo ""
  echo "Available commands:"
  echo "  make build    - Build the fat JAR"
  echo "  make run      - Run the application"
  echo "  make dev      - Build and run"
  echo "  make test     - Run tests"
  echo "  make clean    - Clean build outputs"
  echo "  make frontend - Start Angular dev server"
  echo ""
}

case "$TARGET" in
  build)
    "$MVN" clean package -pl bootstrap -am -DskipTests -q
    ;;
  run)
    [[ -f "$JAR" ]] || { echo "JAR not found at $JAR. Run 'make build' first." >&2; exit 1; }
    java -jar "$JAR"
    ;;
  dev)
    "$0" build
    "$0" run
    ;;
  test)
    "$MVN" test -pl bootstrap -am
    ;;
  clean)
    "$MVN" clean -q
    ;;
  frontend)
    cd frontend
    npm install
    npm start
    ;;
  help)
    show_help
    ;;
  *)
    echo "Unknown target '$TARGET'. Run 'make help' for available commands." >&2
    exit 1
    ;;
esac
