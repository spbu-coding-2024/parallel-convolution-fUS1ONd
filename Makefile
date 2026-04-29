.DEFAULT_GOAL := help

.PHONY: help build test fmt bench plots clean task1 task2 task3

help:
	@echo "Доступные команды:"
	@echo "  make build    — собрать проект"
	@echo "  make test     — прогнать тесты"
	@echo "  make fmt      — отформатировать код (Spotless + ktlint)"
	@echo "  make bench    — запустить JMH-бенчмарки"
	@echo "  make plots    — сгенерить графики из результатов JMH"
	@echo "  make clean    — удалить артефакты сборки"
	@echo "  make task1    — запустить задачу 1 (ARGS=\"...\")"
	@echo "  make task2    — запустить задачу 2 (ARGS=\"...\")"
	@echo "  make task3    — запустить задачу 3 (ARGS=\"...\")"

build: fmt
	./gradlew build -x test

test: fmt
	./gradlew test

fmt:
	./gradlew spotlessApply

bench:
	./gradlew :benchmarks:jmh

plots:
	.venv/bin/python scripts/plot.py

clean:
	./gradlew clean

task1:
	./gradlew :task1:run --args="$(ARGS)"

task2:
	./gradlew :task2:run --args="$(ARGS)"

task3:
	./gradlew :task3:run --args="$(ARGS)"
