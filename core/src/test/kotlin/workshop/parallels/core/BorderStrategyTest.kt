package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BorderStrategyTest :
    StringSpec({
        // Тестовая картинка 3x3:
        //   10 20 30
        //   40 50 60
        //   70 80 90
        val image = Image(3, 3, intArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90))

        "CLAMP внутри границ возвращает пиксель как есть" {
            BorderStrategy.CLAMP.getPixel(image, 1, 1) shouldBe 50
            BorderStrategy.CLAMP.getPixel(image, 0, 0) shouldBe 10
            BorderStrategy.CLAMP.getPixel(image, 2, 2) shouldBe 90
        }

        "CLAMP за левой/верхней границей возвращает крайний пиксель" {
            BorderStrategy.CLAMP.getPixel(image, -1, 0) shouldBe 10
            BorderStrategy.CLAMP.getPixel(image, 0, -1) shouldBe 10
            BorderStrategy.CLAMP.getPixel(image, -5, -5) shouldBe 10
        }

        "CLAMP за правой/нижней границей возвращает крайний пиксель" {
            BorderStrategy.CLAMP.getPixel(image, 3, 0) shouldBe 30
            BorderStrategy.CLAMP.getPixel(image, 0, 3) shouldBe 70
            BorderStrategy.CLAMP.getPixel(image, 100, 100) shouldBe 90
        }
    })
