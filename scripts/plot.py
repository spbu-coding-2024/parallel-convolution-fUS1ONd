"""Генерация графиков из результатов JMH-бенчмарков."""

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

# Подписи для ядер на графике 2.
KERNEL_LABELS = {
    "box-blur": "box-blur\n(3×3)",
    "gaussian": "gaussian\n(3×3)",
    "gaussian-5x5": "gaussian\n(5×5)",
    "motion-blur": "motion-blur\n(9×9)",
}
KERNEL_ORDER = ["box-blur", "gaussian", "gaussian-5x5", "motion-blur"]


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


def main() -> None:
    results = load_results(RESULTS_PATH)
    plot_image_size(results)
    plot_kernel_size(results)


if __name__ == "__main__":
    main()
