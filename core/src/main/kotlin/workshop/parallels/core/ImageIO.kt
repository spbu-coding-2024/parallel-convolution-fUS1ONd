package workshop.parallels.core

import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.file.Path
import javax.imageio.ImageIO as JImageIO

// IO для grayscale-картинок. Использует javax.imageio.
//
// При загрузке цветных PNG/JPG источник конвертируется в TYPE_BYTE_GRAY через drawImage
// (ColorConvertOp применяет gamma-коррекцию sRGB → linear).
// Чтение и запись пикселей идёт напрямую через DataBufferByte — это и быстрее (нет
// per-pixel-обращений через getRGB с цветовой моделью), и обеспечивает точный roundtrip:
// записанный байт яркости загружается обратно в точности тем же.
object ImageIO {
    fun load(path: Path): Image {
        val source =
            JImageIO.read(path.toFile())
                ?: error("не удалось прочитать изображение: $path")
        val gray =
            if (source.type == BufferedImage.TYPE_BYTE_GRAY) {
                source
            } else {
                BufferedImage(source.width, source.height, BufferedImage.TYPE_BYTE_GRAY).also {
                    val g = it.createGraphics()
                    try {
                        g.drawImage(source, 0, 0, null)
                    } finally {
                        g.dispose()
                    }
                }
            }
        val raster = (gray.raster.dataBuffer as DataBufferByte).data
        val pixels = IntArray(gray.width * gray.height) { raster[it].toInt() and 0xFF }
        return Image(gray.width, gray.height, pixels)
    }

    fun save(image: Image, path: Path) {
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_GRAY)
        val raster = (out.raster.dataBuffer as DataBufferByte).data
        for (i in image.pixels.indices) {
            raster[i] = (image.pixels[i] and 0xFF).toByte()
        }
        path.toFile().parentFile?.mkdirs()
        val format = path.toString().substringAfterLast('.', "png").lowercase()
        JImageIO.write(out, format, path.toFile())
    }
}
