package workshop.parallels.core

import java.util.concurrent.Executors
import java.util.concurrent.Future

// Способы разделения изображения между потоками при параллельной свёртке.
enum class ParallelStrategy {
    PER_PIXEL,
    BY_ROWS,
    BY_COLUMNS,
    GRID,
}

// Параллельная свёртка. Семантика идентична convolve() из Convolution.kt.
// threads — число рабочих потоков (>= 1).
// blockWidth/blockHeight — размер плитки для GRID; игнорируется остальными стратегиями.
fun convolveParallel(
    image: Image,
    kernel: Kernel,
    strategy: ParallelStrategy,
    threads: Int = Runtime.getRuntime().availableProcessors(),
    blockWidth: Int = 64,
    blockHeight: Int = 64,
    border: BorderStrategy = BorderStrategy.CLAMP,
): Image = when (strategy) {
    ParallelStrategy.BY_ROWS -> convolveByRows(image, kernel, threads, border)
    ParallelStrategy.BY_COLUMNS -> convolveByColumns(image, kernel, threads, border)
    ParallelStrategy.PER_PIXEL -> convolvePerPixel(image, kernel, threads, border)
    ParallelStrategy.GRID -> convolveByGrid(image, kernel, blockWidth, blockHeight, threads, border)
}

// Запускает задачи через пул потоков, ждёт завершения и гарантированно завершает пул.
// Каждая задача пишет в непересекающийся диапазон result — синхронизация не нужна.
private fun runParallel(image: Image, threads: Int, submitTasks: (result: IntArray, submit: (() -> Unit) -> Future<*>) -> Unit): Image {
    val result = IntArray(image.width * image.height)
    val executor = Executors.newFixedThreadPool(threads)
    val futures = mutableListOf<Future<*>>()
    try {
        submitTasks(result) { task -> executor.submit(task).also { futures += it } }
        futures.forEach { it.get() }
    } finally {
        executor.shutdown()
    }
    return Image(image.width, image.height, result)
}

private fun convolveByRows(image: Image, kernel: Kernel, threads: Int, border: BorderStrategy): Image =
    runParallel(image, threads) { result, submit ->
        val rowsPerThread = maxOf(1, image.height / threads)
        for (t in 0 until threads) {
            val yStart = t * rowsPerThread
            val yEnd = if (t == threads - 1) image.height else minOf(yStart + rowsPerThread, image.height)
            if (yStart >= image.height) break
            submit {
                for (y in yStart until yEnd) {
                    for (x in 0 until image.width) {
                        result[y * image.width + x] = computePixel(image, kernel, x, y, border)
                    }
                }
            }
        }
    }

private fun convolveByColumns(image: Image, kernel: Kernel, threads: Int, border: BorderStrategy): Image =
    runParallel(image, threads) { result, submit ->
        val colsPerThread = maxOf(1, image.width / threads)
        for (t in 0 until threads) {
            val xStart = t * colsPerThread
            val xEnd = if (t == threads - 1) image.width else minOf(xStart + colsPerThread, image.width)
            if (xStart >= image.width) break
            submit {
                for (x in xStart until xEnd) {
                    for (y in 0 until image.height) {
                        result[y * image.width + x] = computePixel(image, kernel, x, y, border)
                    }
                }
            }
        }
    }

private fun convolvePerPixel(image: Image, kernel: Kernel, threads: Int, border: BorderStrategy): Image =
    runParallel(image, threads) { result, submit ->
        for (idx in 0 until image.width * image.height) {
            val x = idx % image.width
            val y = idx / image.width
            submit { result[idx] = computePixel(image, kernel, x, y, border) }
        }
    }

private fun convolveByGrid(image: Image, kernel: Kernel, blockWidth: Int, blockHeight: Int, threads: Int, border: BorderStrategy): Image =
    runParallel(image, threads) { result, submit ->
        val bw = maxOf(1, blockWidth)
        val bh = maxOf(1, blockHeight)
        var bx = 0
        while (bx < image.width) {
            val xStart = bx
            val xEnd = minOf(bx + bw, image.width)
            var by = 0
            while (by < image.height) {
                val yStart = by
                val yEnd = minOf(by + bh, image.height)
                submit {
                    for (y in yStart until yEnd) {
                        for (x in xStart until xEnd) {
                            result[y * image.width + x] = computePixel(image, kernel, x, y, border)
                        }
                    }
                }
                by += bh
            }
            bx += bw
        }
    }

// Вычисляет значение одного пикселя свёртки — та же логика, что в Convolution.kt.
private fun computePixel(image: Image, kernel: Kernel, x: Int, y: Int, border: BorderStrategy): Int {
    val radius = kernel.radius
    var sum = 0.0
    for (ky in -radius..radius) {
        for (kx in -radius..radius) {
            sum += border.getPixel(image, x + kx, y + ky) * kernel.get(kx, ky)
        }
    }
    return (sum * kernel.factor + kernel.bias).toInt().coerceIn(0, 255)
}
