package workshop.parallels.core

// Ядро свёртки. Хранится как одномерный DoubleArray размером size * size в row-major порядке.
// factor применяется к свёрнутой сумме как множитель, bias — как сдвиг.
class Kernel(
    val size: Int,
    val data: DoubleArray,
    val factor: Double = 1.0,
    val bias: Double = 0.0,
) {
    init {
        require(size > 0 && size % 2 == 1) {
            "size должен быть положительным нечётным числом, получено: $size"
        }
        require(data.size == size * size) {
            "размер data (${data.size}) не равен size * size (${size * size})"
        }
    }

    val radius: Int = size / 2

    // Доступ по координатам относительно центра: (-radius..radius, -radius..radius).
    fun get(dx: Int, dy: Int): Double = data[(dy + radius) * size + (dx + radius)]
}
