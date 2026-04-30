package workshop.parallels.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ImageTest :
    StringSpec({
        "конструктор хранит ширину, высоту и пиксели" {
            val image = Image(2, 3, IntArray(6) { it * 10 })
            image.width shouldBe 2
            image.height shouldBe 3
            image.get(0, 0) shouldBe 0
            image.get(1, 2) shouldBe 50
        }

        "get(x, y) возвращает пиксель из row-major массива" {
            // pixels раскладываются построчно: pixels[y * width + x]
            val image = Image(3, 2, intArrayOf(10, 20, 30, 40, 50, 60))
            image.get(0, 0) shouldBe 10
            image.get(2, 0) shouldBe 30
            image.get(0, 1) shouldBe 40
            image.get(2, 1) shouldBe 60
        }

        "конструктор бросает исключение при несоответствии размера массива" {
            shouldThrow<IllegalArgumentException> {
                Image(2, 2, IntArray(3))
            }
        }

        "конструктор бросает исключение при неположительных размерах" {
            shouldThrow<IllegalArgumentException> { Image(0, 1, IntArray(0)) }
            shouldThrow<IllegalArgumentException> { Image(1, 0, IntArray(0)) }
            shouldThrow<IllegalArgumentException> { Image(-1, 1, IntArray(0)) }
        }
    })
