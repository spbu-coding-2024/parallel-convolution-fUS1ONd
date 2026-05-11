package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.forAll
import java.util.concurrent.atomic.AtomicInteger

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

        "bounded queue: число картинок в полёте не превышает queueCap + convWorkers" {
            val img = Image(32, 32, IntArray(32 * 32) { 100 })
            val images = List(20) { img }
            val queueCap = 2
            val convWorkers = 2
            val inFlight = AtomicInteger(0)
            val maxInFlight = AtomicInteger(0)

            pipeline(
                images,
                gaussian,
                convWorkers = convWorkers,
                queueCap = queueCap,
                onAfterRead = {
                    val current = inFlight.incrementAndGet()
                    maxInFlight.updateAndGet { maxOf(it, current) }
                },
                onBeforeConv = {
                    inFlight.decrementAndGet()
                    // имитируем медленную свёртку — reader быстро упрётся в очередь
                    Thread.sleep(20)
                },
            )

            // reader инкрементит до put(), conv декрементит после take() в onBeforeConv —
            // верхняя граница: queueCap + по одной у каждого воркера + одна у reader'а перед put().
            maxInFlight.get() shouldBeLessThanOrEqual queueCap + convWorkers + 1
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
