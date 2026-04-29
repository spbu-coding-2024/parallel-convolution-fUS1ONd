"""Генерация графиков из результатов JMH-бенчмарков."""

import json
import pathlib
import sys

import matplotlib.pyplot as plt

RESULTS_PATH = pathlib.Path("benchmarks/build/results/jmh/results.json")
PLOTS_DIR = pathlib.Path("docs/plots")

# Порядок отображения — от меньшего файла к большему.
IMAGE_ORDER = ["img1", "img2", "img3", "img4", "img5"]

# Порядок отображения — от меньшего ядра к большему (3×3, 3×3, 5×5, 9×9).
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
    """График: среднее время свёртки vs имя картинки (ядро gaussian 3×3)."""
    data = extract_bench(results, "benchImageSize")
    scores: dict[str, float] = {
        r["params"]["imageName"]: r["primaryMetric"]["score"] for r in data
    }

    labels = [img for img in IMAGE_ORDER if img in scores]
    values = [scores[img] for img in labels]

    fig, ax = plt.subplots()
    ax.bar(labels, values)
    ax.set_xlabel("Картинка")
    ax.set_ylabel("Среднее время, мс")
    ax.set_title("Время свёртки vs размер изображения (gaussian 3×3)")

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_image_size.png"
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"Сохранено: {out}")


def plot_kernel_size(results: list[dict]) -> None:
    """График: среднее время свёртки vs имя ядра (картинка img3)."""
    data = extract_bench(results, "benchKernelSize")
    scores: dict[str, float] = {
        r["params"]["kernelName"]: r["primaryMetric"]["score"] for r in data
    }

    labels = [k for k in KERNEL_ORDER if k in scores]
    values = [scores[k] for k in labels]

    fig, ax = plt.subplots()
    ax.bar(labels, values)
    ax.set_xlabel("Ядро")
    ax.set_ylabel("Среднее время, мс")
    ax.set_title("Время свёртки vs размер ядра (img3)")

    PLOTS_DIR.mkdir(parents=True, exist_ok=True)
    out = PLOTS_DIR / "bench_kernel_size.png"
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"Сохранено: {out}")


def main() -> None:
    results = load_results(RESULTS_PATH)
    plot_image_size(results)
    plot_kernel_size(results)


if __name__ == "__main__":
    main()
