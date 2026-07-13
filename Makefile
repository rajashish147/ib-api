# ══════════════════════════════════════════════════════════════════════════════
# IB Trader — Makefile
# Simple commands for build, run, and dev tasks.
#
# Usage:
#   make build    - compile and package the fat JAR (tests skipped)
#   make run      - run the fat JAR (.env loaded automatically)
#   make dev      - build + run in one step
#   make test     - run all tests
#   make clean    - clean all build outputs
#   make frontend - start the Angular dev server
#
# On Windows use:  mvnw.cmd package ...
# On Linux/Mac:    ./mvnw package ...
# ══════════════════════════════════════════════════════════════════════════════

# Detect OS
ifeq ($(OS),Windows_NT)
  MVN := mvnw.cmd
else
  MVN := ./mvnw
endif

JAR := bootstrap/target/ib-trader.jar

.PHONY: build run dev test clean frontend help

## build: Compile and package the fat JAR (tests skipped)
build:
	$(MVN) clean package -pl bootstrap -am -DskipTests -q

## run: Run the application (.env loaded automatically via spring-dotenv)
run:
	java -jar $(JAR)

## dev: Build then run in one step
dev: build run

## test: Run all tests
test:
	$(MVN) test -pl bootstrap -am

## clean: Remove all build artifacts
clean:
	$(MVN) clean -q

## frontend: Start the Angular dev server
frontend:
	cd frontend && npm install && npm start

## help: Show available commands
help:
	@echo ""
	@echo "Available commands:"
	@echo "  make build    - Build the fat JAR"
	@echo "  make run      - Run the application"
	@echo "  make dev      - Build and run"
	@echo "  make test     - Run tests"
	@echo "  make clean    - Clean build outputs"
	@echo "  make frontend - Start Angular dev server"
	@echo ""
