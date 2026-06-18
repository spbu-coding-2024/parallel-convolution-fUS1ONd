package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

// Property-based тесты GPU-свёртки.
// Главное свойство: convolveGpu даёт тот же результат, что и convolve().
class GpuConvolutionTest :
    StringSpec({
        val identity3 = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
        val gaussian = Kernels.GAUSSIAN
        val sharpen = Kernels.SHARPEN

        "identity-ядро не меняет картинку" {
            checkAll(randomImageArb) { image ->
                val actual = convolveGpu(image, identity3)
                actual.pixels.toList() shouldBe image.pixels.toList()
            }
        }

        "результат совпадает с convolve() — gaussian" {
            checkAll(randomImageArb) { image ->
                val expected = convolve(image, gaussian)
                val actual = convolveGpu(image, gaussian)
                actual.pixels.toList() shouldBe expected.pixels.toList()
            }
        }

        "результат совпадает с convolve() — sharpen" {
            checkAll(randomImageArb) { image ->
                val expected = convolve(image, sharpen)
                val actual = convolveGpu(image, sharpen)
                actual.pixels.toList() shouldBe expected.pixels.toList()
            }
        }

        "картинка 1x1 — обработка границ через CLAMP" {
            val img = Image(1, 1, intArrayOf(128))
            convolveGpu(img, identity3).pixels.toList() shouldBe listOf(128)
        }
    })
