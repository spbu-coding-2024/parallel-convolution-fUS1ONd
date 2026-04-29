.DEFAULT_GOAL := help

TASK ?= all

.PHONY: help build test fmt bench _do_bench plots clean task1 task2 task3

help:
	@echo "Доступные команды:"
	@echo "  make build          — собрать проект"
	@echo "  make test           — прогнать тесты"
	@echo "  make fmt            — отформатировать код (Spotless + ktlint)"
	@echo "  make bench          — запустить все JMH-бенчмарки"
	@echo "  make bench TASK=1   — только бенчмарки задачи 1"
	@echo "  make bench TASK=2   — только бенчмарки задачи 2"
	@echo "  make plots          — сгенерить все графики из результатов JMH"
	@echo "  make plots TASK=1   — только графики задачи 1"
	@echo "  make plots TASK=2   — только графики задачи 2"
	@echo "  make clean          — удалить артефакты сборки"
	@echo "  make task1          — запустить задачу 1 (ARGS=\"...\")"
	@echo "  make task2          — запустить задачу 2 (ARGS=\"...\")"
	@echo "  make task3          — запустить задачу 3 (ARGS=\"...\")"

build: fmt
	./gradlew build -x test

test: fmt
	./gradlew test

fmt:
	./gradlew spotlessApply

bench:
	@if [ -f benchmarks/build/results/jmh/results.json ]; then \
		printf "Результаты уже есть. Перезапустить бенчмарки? [y/N] "; \
		read ans; \
		if [ "$$ans" = "y" ] || [ "$$ans" = "Y" ]; then \
			$(MAKE) _do_bench TASK=$(TASK); \
		else \
			echo "Используются существующие результаты."; \
		fi \
	else \
		$(MAKE) _do_bench TASK=$(TASK); \
	fi

_do_bench:
ifeq ($(TASK),1)
	./gradlew :benchmarks:jmh -Pjmh.include=ConvolutionBench --rerun
else ifeq ($(TASK),2)
	./gradlew :benchmarks:jmh -Pjmh.include=ParallelConvolutionBench --rerun
else
	./gradlew :benchmarks:jmh --rerun
endif

plots:
	.venv/bin/python scripts/plot.py --task=$(TASK)

clean:
	./gradlew clean

task1:
	./gradlew :task1:run --args="$(ARGS)"

task2:
	./gradlew :task2:run --args="$(ARGS)"

task3:
	./gradlew :task3:run --args="$(ARGS)"
