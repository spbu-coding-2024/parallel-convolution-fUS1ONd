.DEFAULT_GOAL := help

TASK ?= all

# Каждая задача пишет результаты JMH в свой файл — это позволяет переиспользовать
# данные между запусками (например, plots TASK=2 опирается на baseline из task1).
RESULTS_DIR := benchmarks/build/results/jmh
RESULTS_TASK1 := $(RESULTS_DIR)/results-task1.json
RESULTS_TASK2 := $(RESULTS_DIR)/results-task2.json

ifeq ($(TASK),1)
    RESULTS_FILE := $(RESULTS_TASK1)
else ifeq ($(TASK),2)
    RESULTS_FILE := $(RESULTS_TASK2)
else
    # Для TASK=all считаем «есть результаты», только если есть оба файла.
    RESULTS_FILE := $(RESULTS_TASK1) $(RESULTS_TASK2)
endif

.PHONY: help build test fmt bench _do_bench _bench_task1 _bench_task2 plots clean task1 task2 task3

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
	@have_all=1; for f in $(RESULTS_FILE); do [ -s "$$f" ] || have_all=0; done; \
	if [ $$have_all -eq 1 ]; then \
		printf "Результаты уже есть ($(RESULTS_FILE)). Перезапустить бенчмарки? [y/N] "; \
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
	$(MAKE) _bench_task1
else ifeq ($(TASK),2)
	$(MAKE) _bench_task2
else
	$(MAKE) _bench_task1
	$(MAKE) _bench_task2
endif

_bench_task1:
	./gradlew :benchmarks:jmh \
		"-Pjmh.include=^workshop\\.parallels\\.benchmarks\\.ConvolutionBench\\." \
		"-Pjmh.rff=results-task1.json" --rerun

_bench_task2:
	./gradlew :benchmarks:jmh \
		"-Pjmh.include=^workshop\\.parallels\\.benchmarks\\.ParallelConvolutionBench\\." \
		"-Pjmh.rff=results-task2.json" --rerun

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
