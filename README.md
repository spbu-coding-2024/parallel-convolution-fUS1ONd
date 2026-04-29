# Workshop-parallels

Учебный проект СПбГУ по параллельному программированию: свёртка изображений в градациях серого на Kotlin (JVM). Последовательная реализация, параллельные стратегии, pipeline-обработка.

Полный текст задания — см. [ts.md](ts.md).

## Текущее состояние

- ✅ **Задача 1.** Последовательная свёртка одного изображения.
- ⏳ **Задача 2.** Параллельная свёртка (стратегии разделения, cache locality).
- ⏳ **Задача 3.** Pipeline-обработка массива изображений (producer-consumer).

## Стек

| Компонент   | Версия                                        |
| ----------- | --------------------------------------------- |
| Kotlin      | 2.0.21                                        |
| JDK         | 21 (Temurin / OpenJDK)                        |
| Gradle      | 8.10 (через wrapper)                          |
| JUnit       | 5.11.0                                        |
| Kotest      | 5.9.1 (kotest-runner-junit5, kotest-property) |
| Spotless    | 6.25.0                                        |
| ktlint      | 1.3.1                                         |
| kotlinx-cli | 0.3.6                                         |
| Python      | 3.10+ (для скриптов с графиками)              |

Версии зафиксированы в [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Требования

- Linux (тестировалось на Ubuntu 22.04+)
- JDK 21
- Python 3.10+ с поддержкой `venv`
- GNU Make
- Git

## Установка

### JDK 21

Через apt:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
```

Либо через [SDKMAN!](https://sdkman.io/) (удобно переключать версии):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
```

Проверка: `java -version` должно показать 21.

### Python и виртуальное окружение

```bash
sudo apt install -y python3 python3-venv python3-pip make git
python3 -m venv .venv
.venv/bin/pip install -r scripts/requirements.txt
```

Активация venv не требуется — `make plots` использует `.venv/bin/python` напрямую.

### Gradle

Устанавливать не нужно — wrapper (`./gradlew`) сам подтянет версию 8.10 при первом запуске.

## Быстрый старт

```bash
git clone <repo>
cd Workshop-parallels

# собрать и проверить
make build
make test

# запустить задачу 1 на тестовой картинке
make task1
```

Результат окажется в `out/task1.png`.

## Доступные команды (Makefile)

| Команда                  | Описание                                        |
| ------------------------ | ----------------------------------------------- |
| `make` (или `make help`) | список команд                                   |
| `make build`             | сборка проекта (с авто-`fmt`)                   |
| `make test`              | прогон всех тестов (с авто-`fmt`)               |
| `make fmt`               | форматирование (Spotless + ktlint)              |
| `make clean`             | удалить артефакты сборки                        |
| `make task1 ARGS="..."`  | запустить задачу 1                              |
| `make task2 ARGS="..."`  | запустить задачу 2 (планируется)                |
| `make task3 ARGS="..."`  | запустить задачу 3 (планируется)                |
| `make bench`             | JMH-бенчмарки (планируется)                     |
| `make plots`             | сгенерировать графики из JMH JSON (планируется) |

## Структура проекта

```
Workshop-parallels/
├── core/                       # общая библиотека
│   ├── Image.kt                # grayscale-картинка (IntArray, row-major)
│   ├── Kernel.kt               # ядро свёртки (size, data, factor, bias)
│   ├── BorderStrategy.kt       # обработка границ (CLAMP)
│   ├── Convolution.kt          # последовательная свёртка
│   ├── Kernels.kt              # каталог стандартных ядер
│   └── ImageIO.kt              # загрузка/сохранение через javax.imageio
├── task1/                      # CLI задачи 1 (kotlinx-cli)
├── samples/                    # тестовые картинки
├── scripts/                    # скрипты (графики)
├── docs/                       # анализ производительности по задачам
├── gradle/libs.versions.toml   # version catalog
├── Makefile                    # точки входа
└── ts.md                       # текст задания
```

## Запуск задачи 1

```bash
# с дефолтами: gaussian blur на samples/img1.jpg → out/task1.png
make task1

# с параметрами
make task1 ARGS="-i samples/img4.jpg -o out/sharpen.png -k sharpen"
```

CLI-флаги:

| Флаг | Длинный    | Дефолт             | Описание                 |
| ---- | ---------- | ------------------ | ------------------------ |
| `-i` | `--input`  | `samples/img1.jpg` | путь к входной картинке  |
| `-o` | `--output` | `out/task1.png`    | путь к выходной картинке |
| `-k` | `--kernel` | `gaussian`         | имя ядра                 |

Поддерживаемые форматы: PNG, JPG. Цветные картинки автоматически конвертируются в grayscale при загрузке.

## Доступные фильтры

| Имя        | Размер | Эффект                                          |
| ---------- | ------ | ----------------------------------------------- |
| `identity` | 3×3    | тождественное преобразование (без изменений)    |
| `box-blur` | 3×3    | равномерное усреднение по 9 соседям             |
| `gaussian` | 3×3    | гауссово размытие (центр весомее краёв)         |
| `sharpen`  | 3×3    | повышение резкости (усиление перепадов яркости) |

Каталог пополняется — см. [`core/.../Kernels.kt`](core/src/main/kotlin/workshop/parallels/core/Kernels.kt).

### Обработка границ

Реализована стратегия **CLAMP**: пиксели за границей берутся как ближайший крайний. Архитектура (`enum BorderStrategy`) допускает добавление других стратегий (zero-padding, mirror, wrap) без изменения API свёртки.

## Тестирование

Запуск всех тестов:

```bash
make test
```

Покрытие:

- **Unit-тесты** для `Image`, `Kernel`, `BorderStrategy`, `Kernels`, `ImageIO`.
- **Property-based тесты** свёртки (Kotest `checkAll`) — реализованы все 5 свойств из ТЗ:
  1. `id`-фильтр не меняет картинку
  2. нулевой фильтр даёт чёрную картинку
  3. композициональность: `convolve(convolve(img, k1), k2) == convolve(img, k1∘k2)`
  4. расширение ядра нулями не меняет результат
  5. сдвиг и обратный сдвиг возвращают исходную картинку (внутри границ)
- **Roundtrip-тест** для `ImageIO`: `save → load` возвращает тот же `IntArray` (точное совпадение благодаря работе через `DataBufferByte`).

## Анализ производительности

2Подробный разбор по каждой задаче — в отдельных документах:

- [Задача 1 — последовательная свёртка](docs/task1.md) _(в работе)_
- [Задача 2 — параллельные стратегии](docs/task2.md) _(в работе)_
- [Задача 3 — pipeline](docs/task3.md) _(в работе)_

_Графики и краткие выводы появятся после реализации JMH-бенчмарков._

## Языковые соглашения

- Комментарии в коде — на русском.
- Названия классов, методов, переменных — на английском (стандарт Kotlin).
- Сообщения коммитов — Conventional Commits на русском (`feat:`, `fix:`, `chore:`, `init:`).
