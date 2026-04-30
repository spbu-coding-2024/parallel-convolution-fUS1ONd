package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class KernelsTest :
    StringSpec({
        "каталог по имени содержит все ожидаемые ядра" {
            Kernels.byName.keys shouldBe
                setOf(
                    "identity", "box-blur", "gaussian", "sharpen",
                    "gaussian-5x5", "motion-blur", "edge-detection", "sharpen-5x5", "emboss", "emboss-5x5",
                )
        }

        "сумма коэффициентов BOX_BLUR с factor даёт 1 (сохранение яркости)" {
            val sum = Kernels.BOX_BLUR.data.sum() * Kernels.BOX_BLUR.factor
            sum shouldBe (1.0 plusOrMinus 1e-9)
        }

        "сумма коэффициентов GAUSSIAN с factor даёт 1" {
            val sum = Kernels.GAUSSIAN.data.sum() * Kernels.GAUSSIAN.factor
            sum shouldBe (1.0 plusOrMinus 1e-9)
        }

        "BOX_BLUR на однородной картинке возвращает ту же яркость" {
            val img = Image(5, 5, IntArray(25) { 120 })
            val result = convolve(img, Kernels.BOX_BLUR)
            // Внутри: 120; на краях через CLAMP — тоже 120 (т.к. соседи такие же).
            result.pixels.all { it == 120 } shouldBe true
        }

        // --- Новые ядра ---

        "каталог по имени содержит все новые ядра" {
            val expected =
                setOf(
                    "identity", "box-blur", "gaussian", "sharpen",
                    "gaussian-5x5", "motion-blur", "edge-detection", "sharpen-5x5", "emboss", "emboss-5x5",
                )
            Kernels.byName.keys shouldBe expected
        }

        "GAUSSIAN_5X5 — сумма коэффициентов с factor даёт 1" {
            val sum = Kernels.GAUSSIAN_5X5.data.sum() * Kernels.GAUSSIAN_5X5.factor
            sum shouldBe (1.0 plusOrMinus 1e-9)
        }

        "GAUSSIAN_5X5 — размер ядра 5" {
            Kernels.GAUSSIAN_5X5.size shouldBe 5
        }

        "MOTION_BLUR — размер ядра 9" {
            Kernels.MOTION_BLUR.size shouldBe 9
        }

        "MOTION_BLUR — сумма коэффициентов с factor даёт 1 (сохранение яркости)" {
            val sum = Kernels.MOTION_BLUR.data.sum() * Kernels.MOTION_BLUR.factor
            sum shouldBe (1.0 plusOrMinus 1e-9)
        }

        "EDGE_DETECTION — сумма коэффициентов равна 0 (обнаружение краёв)" {
            val sum = Kernels.EDGE_DETECTION.data.sum()
            sum shouldBe (0.0 plusOrMinus 1e-9)
        }

        "SHARPEN_5X5 — сумма коэффициентов с factor даёт 1" {
            val sum = Kernels.SHARPEN_5X5.data.sum() * Kernels.SHARPEN_5X5.factor
            sum shouldBe (1.0 plusOrMinus 1e-9)
        }

        "SHARPEN_5X5 — размер ядра 5" {
            Kernels.SHARPEN_5X5.size shouldBe 5
        }

        "EMBOSS — применение к однородной картинке даёт пиксели близко к bias (128)" {
            val img = Image(5, 5, IntArray(25) { 100 })
            val result = convolve(img, Kernels.EMBOSS)
            // Однородный фон: вклад данных ≈ 0, остаётся только bias=128, clamp → 128
            result.pixels.all { it == 128 } shouldBe true
        }

        "EMBOSS_5X5 — применение к однородной картинке даёт пиксели близко к bias (128)" {
            val img = Image(7, 7, IntArray(49) { 200 })
            val result = convolve(img, Kernels.EMBOSS_5X5)
            result.pixels.all { it == 128 } shouldBe true
        }
    })
