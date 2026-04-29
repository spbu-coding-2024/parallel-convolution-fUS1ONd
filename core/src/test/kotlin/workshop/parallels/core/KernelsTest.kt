package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class KernelsTest :
    StringSpec({
        "каталог по имени содержит все ожидаемые ядра" {
            Kernels.byName.keys shouldBe setOf("identity", "box-blur", "gaussian", "sharpen")
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
    })
