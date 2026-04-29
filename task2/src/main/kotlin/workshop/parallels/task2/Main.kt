package workshop.parallels.task2

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import workshop.parallels.core.ImageIO
import workshop.parallels.core.Kernels
import workshop.parallels.core.ParallelStrategy
import workshop.parallels.core.convolveParallel
import java.nio.file.Path
import kotlin.system.measureTimeMillis

// CLI задачи 2: параллельная свёртка с выбором стратегии и числа потоков.
fun main(args: Array<String>) {
    val parser = ArgParser("task2")
    val input by parser
        .option(ArgType.String, shortName = "i", description = "путь к входной картинке")
        .default("samples/img1.jpg")
    val output by parser
        .option(ArgType.String, shortName = "o", description = "путь к выходной картинке")
        .default("out/task2.png")
    val kernelName by parser
        .option(
            ArgType.Choice(Kernels.byName.keys.toList(), { it }),
            shortName = "k",
            description = "имя ядра",
        ).default("gaussian")
    val strategyName by parser
        .option(
            ArgType.Choice(listOf("per-pixel", "by-rows", "by-columns", "grid"), { it }),
            shortName = "s",
            description = "стратегия разделения: per-pixel|by-rows|by-columns|grid",
        ).default("by-rows")
    val threads by parser
        .option(ArgType.Int, shortName = "t", description = "число потоков (0 = availableProcessors)")
        .default(0)
    val blockWidth by parser
        .option(ArgType.Int, fullName = "block-width", description = "ширина блока для grid")
        .default(64)
    val blockHeight by parser
        .option(ArgType.Int, fullName = "block-height", description = "высота блока для grid")
        .default(64)
    parser.parse(args)

    val kernel = Kernels.byName.getValue(kernelName)
    val strategy =
        when (strategyName) {
            "per-pixel" -> ParallelStrategy.PER_PIXEL
            "by-rows" -> ParallelStrategy.BY_ROWS
            "by-columns" -> ParallelStrategy.BY_COLUMNS
            "grid" -> ParallelStrategy.GRID
            else -> error("Неизвестная стратегия: $strategyName")
        }
    val nThreads = if (threads == 0) Runtime.getRuntime().availableProcessors() else threads
    val inputPath = Path.of(input)
    val outputPath = Path.of(output)

    println("вход:      $inputPath")
    println("выход:     $outputPath")
    println("ядро:      $kernelName")
    println("стратегия: $strategyName")
    println("потоки:    $nThreads")

    val image = ImageIO.load(inputPath)
    println("размер:    ${image.width}x${image.height}")

    lateinit var result: workshop.parallels.core.Image
    val ms =
        measureTimeMillis {
            result = convolveParallel(image, kernel, strategy, nThreads, blockWidth, blockHeight)
        }
    outputPath.parent?.let {
        java.nio.file.Files
            .createDirectories(it)
    }
    ImageIO.save(result, outputPath)

    println("свёртка:   $ms мс")
}
