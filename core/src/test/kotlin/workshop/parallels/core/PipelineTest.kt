package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.forAll

class PipelineTest :
    StringSpec({
        val gaussian = Kernels.GAUSSIAN

        "pipeline даёт тот же результат, что images.map { convolve }" {
            checkAll(randomImageArb) { image ->
                val images = listOf(image, image, image)
                val expected = images.map { convolve(it, gaussian) }
                val actual = pipeline(images, gaussian, convWorkers = 4, queueCap = 2)
                actual.size shouldBe expected.size
                for (i in actual.indices) {
                    actual[i].pixels.toList() shouldBe expected[i].pixels.toList()
                }
            }
        }

        "pipeline сохраняет порядок картинок" {
            forAll(randomImageArb, randomImageArb, randomImageArb) { a, b, c ->
                val expected = listOf(a, b, c).map { convolve(it, gaussian) }
                val actual = pipeline(listOf(a, b, c), gaussian, convWorkers = 4, queueCap = 2)
                actual.zip(expected).all { (got, exp) -> got.pixels.toList() == exp.pixels.toList() }
            }
        }

        "pipeline на пустом списке возвращает пустой список" {
            pipeline(emptyList(), gaussian) shouldBe emptyList()
        }

        "pipeline с innerStrategy=BY_ROWS даёт тот же результат" {
            checkAll(randomImageArb) { image ->
                val images = listOf(image, image)
                val expected = images.map { convolve(it, gaussian) }
                val actual = pipeline(images, gaussian, convWorkers = 2, queueCap = 2, innerStrategy = ParallelStrategy.BY_ROWS)
                for (i in actual.indices) {
                    actual[i].pixels.toList() shouldBe expected[i].pixels.toList()
                }
            }
        }
    })
