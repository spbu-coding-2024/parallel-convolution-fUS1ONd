package workshop.parallels.core

// Grayscale-картинка. Пиксели хранятся в одномерном IntArray в row-major порядке:
// pixels[y * width + x]. Один большой массив (а не Array<IntArray>) даёт лучшую
// cache locality — это важно для бенчмарков задачи 2.
class Image(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(width > 0) { "width должен быть положительным, получено: $width" }
        require(height > 0) { "height должен быть положительным, получено: $height" }
        require(pixels.size == width * height) {
            "размер pixels (${pixels.size}) не равен width * height (${width * height})"
        }
    }

    fun get(x: Int, y: Int): Int = pixels[y * width + x]
}
