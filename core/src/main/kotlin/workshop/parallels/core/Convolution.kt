package workshop.parallels.core

// Последовательная свёртка изображения с заданным ядром.
// Возвращает новый Image, исходный не меняется.
//
// Формула: result(x, y) = factor · Σᵢⱼ image(x+i, y+j) · kernel(i, j) + bias.
// За границами изображения значения берутся согласно стратегии border (по умолчанию CLAMP).
// Итог клампится в [0, 255].
fun convolve(image: Image, kernel: Kernel, border: BorderStrategy = BorderStrategy.CLAMP): Image {
    val width = image.width
    val height = image.height
    val radius = kernel.radius
    val factor = kernel.factor
    val bias = kernel.bias
    val result = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0.0
            for (ky in -radius..radius) {
                for (kx in -radius..radius) {
                    val pixel = border.getPixel(image, x + kx, y + ky)
                    val coeff = kernel.get(kx, ky)
                    sum += pixel * coeff
                }
            }
            val value = (sum * factor + bias).toInt().coerceIn(0, 255)
            result[y * width + x] = value
        }
    }
    return Image(width, height, result)
}
