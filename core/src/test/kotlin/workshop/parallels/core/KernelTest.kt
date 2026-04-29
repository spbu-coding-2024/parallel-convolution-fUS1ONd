package workshop.parallels.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class KernelTest :
    StringSpec({
        "конструктор принимает квадратное ядро нечётного размера" {
            val k = Kernel(3, DoubleArray(9) { 1.0 })
            k.size shouldBe 3
            k.factor shouldBe 1.0
            k.bias shouldBe 0.0
        }

        "get(i, j) возвращает коэффициент по относительным координатам от центра" {
            // Ядро 3x3:  [a b c]
            //            [d e f]
            //            [g h i]
            // центр = e. Координаты: (-1..1, -1..1).
            val k = Kernel(3, doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0))
            k.get(-1, -1) shouldBe 1.0
            k.get(0, -1) shouldBe 2.0
            k.get(0, 0) shouldBe 5.0
            k.get(1, 1) shouldBe 9.0
        }

        "конструктор бросает исключение при чётном размере" {
            shouldThrow<IllegalArgumentException> { Kernel(2, DoubleArray(4)) }
            shouldThrow<IllegalArgumentException> { Kernel(4, DoubleArray(16)) }
        }

        "конструктор бросает исключение при несоответствии размера массива" {
            shouldThrow<IllegalArgumentException> { Kernel(3, DoubleArray(5)) }
        }

        "factor и bias задаются опционально" {
            val k = Kernel(3, DoubleArray(9), factor = 0.5, bias = 128.0)
            k.factor shouldBe 0.5
            k.bias shouldBe 128.0
        }
    })
