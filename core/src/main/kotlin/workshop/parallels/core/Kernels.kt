package workshop.parallels.core

// Каталог стандартных ядер свёртки. Минимальный набор для задачи 1.
// Расширяется по мере необходимости.
object Kernels {
    // Тождественное ядро: единица в центре. Свёртка не меняет картинку.
    val IDENTITY: Kernel = Kernel(3, doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0))

    // Box blur 3x3: усреднение по 9 соседям.
    val BOX_BLUR: Kernel = Kernel(3, DoubleArray(9) { 1.0 }, factor = 1.0 / 9.0)

    // Гауссово размытие 3x3: центр весомее краёв.
    val GAUSSIAN: Kernel = Kernel(3, doubleArrayOf(1.0, 2.0, 1.0, 2.0, 4.0, 2.0, 1.0, 2.0, 1.0), factor = 1.0 / 16.0)

    // Повышение резкости: усиливает разницу пикселя с соседями.
    val SHARPEN: Kernel = Kernel(3, doubleArrayOf(0.0, -1.0, 0.0, -1.0, 5.0, -1.0, 0.0, -1.0, 0.0))

    // Доступ по имени для CLI.
    val byName: Map<String, Kernel> =
        mapOf(
            "identity" to IDENTITY,
            "box-blur" to BOX_BLUR,
            "gaussian" to GAUSSIAN,
            "sharpen" to SHARPEN,
        )
}
