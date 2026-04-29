package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

// Property-based тесты параллельной свёртки.
// Главное свойство: любая стратегия даёт тот же результат, что и convolve().
class ParallelConvolutionTest :
    StringSpec({
        val identity3 = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
        val gaussian = Kernels.GAUSSIAN

        for (strategy in ParallelStrategy.entries) {
            "[${strategy.name}] результат совпадает с convolve() на случайных картинках" {
                checkAll(randomImageArb) { image ->
                    val expected = convolve(image, identity3)
                    val actual = convolveParallel(image, identity3, strategy, threads = 4)
                    actual.pixels.toList() shouldBe expected.pixels.toList()
                }
            }

            "[${strategy.name}] threads=1 совпадает с последовательной реализацией" {
                checkAll(randomImageArb) { image ->
                    val expected = convolve(image, gaussian)
                    val actual = convolveParallel(image, gaussian, strategy, threads = 1)
                    actual.pixels.toList() shouldBe expected.pixels.toList()
                }
            }
        }

        // Граничный случай: картинка 1x1 не падает
        "[PER_PIXEL] картинка 1x1 с 4 потоками" {
            val img = Image(1, 1, intArrayOf(128))
            convolveParallel(img, identity3, ParallelStrategy.PER_PIXEL, threads = 4)
                .pixels.toList() shouldBe listOf(128)
        }

        // GRID: блок больше картинки — должна остаться одна задача
        "[GRID] blockWidth и blockHeight больше размера картинки" {
            checkAll(randomImageArb) { image ->
                val expected = convolve(image, gaussian)
                val actual =
                    convolveParallel(
                        image,
                        gaussian,
                        ParallelStrategy.GRID,
                        threads = 4,
                        blockWidth = 1000,
                        blockHeight = 1000,
                    )
                actual.pixels.toList() shouldBe expected.pixels.toList()
            }
        }

        "[GRID] blockWidth=1 blockHeight=1 — максимальное дробление" {
            checkAll(randomImageArb) { image ->
                val expected = convolve(image, gaussian)
                val actual =
                    convolveParallel(
                        image,
                        gaussian,
                        ParallelStrategy.GRID,
                        threads = 4,
                        blockWidth = 1,
                        blockHeight = 1,
                    )
                actual.pixels.toList() shouldBe expected.pixels.toList()
            }
        }
    })
