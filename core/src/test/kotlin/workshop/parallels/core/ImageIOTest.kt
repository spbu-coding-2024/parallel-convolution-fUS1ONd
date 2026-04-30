package workshop.parallels.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe

class ImageIOTest :
    StringSpec({
        "save потом load возвращает ту же grayscale-картинку (PNG)" {
            val dir = tempdir()
            val path = dir.toPath().resolve("test.png")
            val original = Image(4, 3, intArrayOf(0, 50, 100, 150, 200, 250, 25, 75, 125, 175, 225, 255))
            ImageIO.save(original, path)
            val loaded = ImageIO.load(path)

            loaded.width shouldBe original.width
            loaded.height shouldBe original.height
            loaded.pixels.toList() shouldBe original.pixels.toList()
        }

        "load на отсутствующем файле бросает исключение" {
            val dir = tempdir()
            val path = dir.toPath().resolve("nope.png")
            try {
                ImageIO.load(path)
                error("ожидалось исключение")
            } catch (e: Exception) {
                // ok
            }
        }
    })
