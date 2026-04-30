package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

// Property-based тесты по списку из README (раздел "Схема тестирования").
// Все свойства проверяются на случайных картинках и/или ядрах.
class ConvolutionTest :
    StringSpec({
        val identity3 = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0))
        val zero3 = Kernel(3, DoubleArray(9))

        "[свойство 1] id-фильтр не меняет картинку" {
            checkAll(randomImageArb) { image ->
                val result = convolve(image, identity3)
                result.width shouldBe image.width
                result.height shouldBe image.height
                result.pixels.toList() shouldBe image.pixels.toList()
            }
        }

        "[свойство 2] нулевой фильтр даёт чёрную картинку" {
            checkAll(randomImageArb) { image ->
                convolve(image, zero3).pixels.all { it == 0 } shouldBe true
            }
        }

        "[свойство 3] композициональность: convolve(convolve(img, k1), k2) == convolve(img, k1∘k2)" {
            // shiftRight: коэффициент 1 в позиции (-1, 0) → новый пиксель = левый сосед.
            // shiftDown:  коэффициент 1 в позиции ( 0,-1) → новый пиксель = верхний сосед.
            // shiftDiag:  композиция = смотрим в верхне-левый сосед.
            val shiftRight = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            val shiftDown = Kernel(3, doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            val shiftDiag = Kernel(3, doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

            checkAll(randomImageArb) { image ->
                val sequential = convolve(convolve(image, shiftRight), shiftDown)
                val composed = convolve(image, shiftDiag)
                sequential.pixels.toList() shouldBe composed.pixels.toList()
            }
        }

        "[свойство 4] расширение ядра нулями не меняет результат" {
            // Sobel-X 3x3 vs то же ядро, обёрнутое в 5x5 нулей.
            val k3 = Kernel(3, doubleArrayOf(-1.0, 0.0, 1.0, -2.0, 0.0, 2.0, -1.0, 0.0, 1.0))
            val k5 =
                Kernel(
                    5,
                    doubleArrayOf(
                        0.0, 0.0, 0.0, 0.0, 0.0,
                        0.0, -1.0, 0.0, 1.0, 0.0,
                        0.0, -2.0, 0.0, 2.0, 0.0,
                        0.0, -1.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0, 0.0,
                    ),
                )
            checkAll(randomImageArb) { image ->
                convolve(image, k3).pixels.toList() shouldBe convolve(image, k5).pixels.toList()
            }
        }

        "[свойство 5] сдвиг и обратный сдвиг возвращают исходную картинку (внутри границ)" {
            // На границах CLAMP может слегка "размазать" — поэтому проверяем внутреннюю область.
            val shiftRight = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            val shiftLeft = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0))
            checkAll(randomImageArb) { image ->
                val roundtrip = convolve(convolve(image, shiftRight), shiftLeft)
                for (y in 1 until image.height - 1) {
                    for (x in 1 until image.width - 1) {
                        roundtrip.get(x, y) shouldBe image.get(x, y)
                    }
                }
            }
        }

        // === Дополнительные unit-тесты на конкретные значения ===

        "свёртка с identity точно сохраняет пиксель в центре" {
            val img = Image(3, 3, intArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90))
            convolve(img, identity3).get(1, 1) shouldBe 50
        }

        "factor применяется как множитель к сумме" {
            val boxBlur = Kernel(3, DoubleArray(9) { 1.0 }, factor = 1.0 / 9.0)
            val img = Image(3, 3, IntArray(9) { 90 })
            convolve(img, boxBlur).get(1, 1) shouldBe 90
        }

        "bias добавляется к результату" {
            val zeroSum = Kernel(3, DoubleArray(9), bias = 100.0)
            convolve(Image(3, 3, IntArray(9) { 50 }), zeroSum).get(1, 1) shouldBe 100
        }

        "результат клампится в [0, 255]" {
            val brighten = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0))
            val img = Image(3, 3, IntArray(9) { 100 })
            convolve(img, brighten).get(1, 1) shouldBe 255

            val darken = Kernel(3, DoubleArray(9), bias = -50.0)
            convolve(img, darken).get(1, 1) shouldBe 0
        }
    })

// Генератор случайных картинок небольшого размера (для скорости тестов).
private val randomImageArb =
    arbitrary { rs ->
        val width = Arb.int(2..16).bind()
        val height = Arb.int(2..16).bind()
        val pixels = IntArray(width * height) { Arb.int(0..255).bind() }
        Image(width, height, pixels)
    }
