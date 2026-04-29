"""Генерация графиков из результатов JMH-бенчмарков."""

import argparse
import json
import pathlib
import sys

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

RESULTS_PATH = pathlib.Path("benchmarks/build/results/jmh/results.json")
PLOTS_DIR = pathlib.Path("docs/plots")

# Реальные размеры картинок в пикселях (сторона квадрата).
IMAGE_SIZES = {
    "img1": 256,
    "img2": 512,
    "img3": 889,
    "img4": 1024,
    "img5": 2048,
}

# Подписи для ядер на графике task1.
KERNEL_LABELS = {
    "box-blur": "box-blur\n(3×3)",
    "gaussian": "gaussian\n(3×3)",
    "gaussian-5x5": "gaussian\n(5×5)",
    "motion-blur": "motion-blur\n(9×9)",
}
KERNEL_ORDER = ["box-blur", "gaussian", "gaussian-5x5", "motion-blur"]

# Порядок и подписи стратегий для графиков task2.
STRATEGY_ORDER = ["SEQUENTIAL", "PER_PIXEL", "BY_ROWS", "BY_COLUMNS", "GRID"]
STRATEGY_LABELS = {
    "SEQUENTIAL": "sequential",
    "PER_PIXEL": "per-pixel",
    "BY_ROWS": "by-rows",
    "BY_COLUMNS": "by-columns",
    "GRID": "grid",
}


def load_results(path: pathlib.Path) -> list[dict]:
    if not path.exists():
        print(f"Файл результатов не найден: {path}", file=sys.stderr)
        print("Запустите 'make bench' перед 'make plots'.", file=sys.stderr)
        sys.exit(1)
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def extract_bench(results: list[dict], method_suffix: str) -> list[dict]:
    return [r for r in results if r["benchmark"].endswith(method_suffix)]


def plot_image_size(results: list[dict]) -> None:
    """График 1: время свёртки vs размер изображения (gaussian 3×3)."""
    data = extract_bench(results, "benchImageSize")
    if not data:
        print("Нет данных для benchImageSize, пропускаю.", file=sys.stderr)
        return

    points = sorted(
        [(IMAGE_SIZES[r["params"]["imageName"]], r["primaryMetric"]["score"]) for r in data],
        key=lambda p: p[0],
    )
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(xs, ys, marker="o", linewidth=2, markersize=7, color="#2196F3", label="gaussian (3×3)")

    ax.set_xlabel("Размер изображения (сторона, px)", fontsize=12)
    ax.set_ylabel("Среднее время, мс", fontsize=12)
    ax.set_title("Время свёртки vs размер изображения", fontsize=13)
    ax.set_xticks(xs)
    ax.xaxis.set_major_formatter(ticker.FuncFormatter(lambda v, _: f"{int(v)}"))
    ax.set_ylim(0, max(ys) * 1.15)

    # Подпись значения у каждой точки.
    for x, y in zip(xs, ys):
        ax.annotate(
            f"{y:.1f}",
            xy=(x, y),
            xytext=(0, 8),
            textcoords="offset points",
            ha="center",
            fontsize=9,
        )

    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.5)
    fig.tight_layout()

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_image_size.png"
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"Сохранено: {out}")


def plot_kernel_size(results: list[dict]) -> None:
    """График 2: время свёртки vs размер ядра (img3, 889×889)."""
    data = extract_bench(results, "benchKernelSize")
    if not data:
        print("Нет данных для benchKernelSize, пропускаю.", file=sys.stderr)
        return

    scores = {r["params"]["kernelName"]: r["primaryMetric"]["score"] for r in data}
    labels = [KERNEL_LABELS[k] for k in KERNEL_ORDER if k in scores]
    values = [scores[k] for k in KERNEL_ORDER if k in scores]

    colors = ["#42A5F5", "#66BB6A", "#FFA726", "#EF5350"]
    fig, ax = plt.subplots(figsize=(8, 5))
    bars = ax.bar(labels, values, color=colors[: len(values)], width=0.5, edgecolor="white")

    # Подпись значения над каждым столбцом.
    for bar, val in zip(bars, values):
        ax.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height() + max(values) * 0.01,
            f"{val:.1f} мс",
            ha="center",
            va="bottom",
            fontsize=10,
        )

    ax.set_xlabel("Ядро свёртки", fontsize=12)
    ax.set_ylabel("Среднее время, мс", fontsize=12)
    ax.set_title("Время свёртки vs размер ядра (изображение 1024×1024)", fontsize=13)
    ax.set_ylim(0, max(values) * 1.15)
    ax.grid(True, axis="y", linestyle="--", alpha=0.5)
    fig.tight_layout()

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_kernel_size.png"
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"Сохранено: {out}")


def plot_parallel_strategies(results: list[dict]) -> None:
    """График 3: время vs стратегия разделения (img4, gaussian, N потоков)."""
    data = extract_bench(results, "benchStrategy")
    if not data:
        print("Нет данных для benchStrategy, пропускаю.", file=sys.stderr)
        return

    scores = {r["params"]["strategyName"]: r["primaryMetric"]["score"] for r in data}
    labels = [STRATEGY_LABELS[k] for k in STRATEGY_ORDER if k in scores]
    values = [scores[k] for k in STRATEGY_ORDER if k in scores]

    colors = ["#90A4AE", "#EF5350", "#42A5F5", "#66BB6A", "#FFA726"]
    fig, ax = plt.subplots(figsize=(9, 5))
    bars = ax.bar(labels, values, color=colors[: len(values)], width=0.5, edgecolor="white")

    for bar, val in zip(bars, values):
        ax.text(
            bar.get_x() + bar.get_width() / 2,
            bar.get_height() + max(values) * 0.01,
            f"{val:.1f} мс",
            ha="center",
            va="bottom",
            fontsize=10,
        )

    ax.set_xlabel("Стратегия", fontsize=12)
    ax.set_ylabel("Среднее время, мс", fontsize=12)
    ax.set_title("Время свёртки vs стратегия разделения (1024×1024, gaussian)", fontsize=13)
    ax.set_ylim(0, max(values) * 1.15)
    ax.grid(True, axis="y", linestyle="--", alpha=0.5)
    fig.tight_layout()

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_parallel_strategies.png"
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"Сохранено: {out}")


def plot_parallel_thread_scaling(results: list[dict]) -> None:
    """График 4: время vs число потоков (BY_ROWS, img4, gaussian)."""
    data = extract_bench(results, "benchThreadCount")
    if not data:
        print("Нет данных для benchThreadCount, пропускаю.", file=sys.stderr)
        return

    points = sorted(
        [(int(r["params"]["threads"]), r["primaryMetric"]["score"]) for r in data],
        key=lambda p: p[0],
    )
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(xs, ys, marker="o", linewidth=2, markersize=7, color="#42A5F5", label="by-rows")

    for x, y in zip(xs, ys):
        ax.annotate(
            f"{y:.1f}",
            xy=(x, y),
            xytext=(0, 8),
            textcoords="offset points",
            ha="center",
            fontsize=9,
        )

    ax.set_xlabel("Число потоков", fontsize=12)
    ax.set_ylabel("Среднее время, мс", fontsize=12)
    ax.set_title("Масштабируемость по числу потоков (by-rows, 1024×1024)", fontsize=13)
    ax.set_xticks(xs)
    ax.set_ylim(0, max(ys) * 1.15)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.5)
    fig.tight_layout()

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_parallel_thread_scaling.png"
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"Сохранено: {out}")


def plot_parallel_image_size(results: list[dict]) -> None:
    """График 5: parallel by-rows vs sequential — время vs размер изображения."""
    parallel_data = extract_bench(results, "benchParallelImageSize")
    seq_data = extract_bench(results, "benchImageSize")
    if not parallel_data:
        print("Нет данных для benchParallelImageSize, пропускаю.", file=sys.stderr)
        return

    par_points = sorted(
        [(IMAGE_SIZES[r["params"]["imageName"]], r["primaryMetric"]["score"]) for r in parallel_data],
        key=lambda p: p[0],
    )

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.plot(
        [p[0] for p in par_points],
        [p[1] for p in par_points],
        marker="o",
        linewidth=2,
        markersize=7,
        color="#42A5F5",
        label="parallel by-rows",
    )

    if seq_data:
        seq_points = sorted(
            [(IMAGE_SIZES[r["params"]["imageName"]], r["primaryMetric"]["score"]) for r in seq_data],
            key=lambda p: p[0],
        )
        ax.plot(
            [p[0] for p in seq_points],
            [p[1] for p in seq_points],
            marker="s",
            linewidth=2,
            markersize=7,
            color="#EF5350",
            linestyle="--",
            label="sequential",
        )

    ax.set_xlabel("Размер изображения (сторона, px)", fontsize=12)
    ax.set_ylabel("Среднее время, мс", fontsize=12)
    ax.set_title("Parallel vs sequential: время vs размер изображения", fontsize=13)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.5)
    fig.tight_layout()

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_parallel_image_size.png"
    fig.savefig(out, dpi=150)
    plt.close(fig)
    print(f"Сохранено: {out}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Генерация графиков из JMH-результатов.")
    parser.add_argument(
        "--task",
        choices=["1", "2", "all"],
        default="all",
        help="Какие графики генерировать: 1, 2 или all (по умолчанию all)",
    )
    args = parser.parse_args()

    results = load_results(RESULTS_PATH)

    if args.task in ("1", "all"):
        plot_image_size(results)
        plot_kernel_size(results)

    if args.task in ("2", "all"):
        plot_parallel_strategies(results)
        plot_parallel_thread_scaling(results)
        plot_parallel_image_size(results)


if __name__ == "__main__":
    main()
