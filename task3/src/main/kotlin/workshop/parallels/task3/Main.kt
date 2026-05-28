package workshop.parallels.task3

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import workshop.parallels.core.ImageIO
import workshop.parallels.core.Kernels
import workshop.parallels.core.ParallelStrategy
import workshop.parallels.core.pipeline
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

// CLI задачи 3: pipeline-обработка папки с картинками (reader -> conv -> writer).
fun main(args: Array<String>) {
    val parser = ArgParser("task3")
    val inputDir by parser
        .option(ArgType.String, fullName = "input-dir", shortName = "i", description = "папка с входными картинками")
        .default("samples")
    val outputDir by parser
        .option(ArgType.String, fullName = "output-dir", shortName = "o", description = "папка для результатов")
        .default("out/task3")
    val kernelName by parser
        .option(
            ArgType.Choice(Kernels.byName.keys.toList(), { it }),
            shortName = "k",
            description = "имя ядра",
        ).default("gaussian")
    val convWorkers by parser
        .option(ArgType.Int, fullName = "conv-workers", description = "число воркеров свёртки (0 = availableProcessors)")
        .default(0)
    val queueCap by parser
        .option(ArgType.Int, fullName = "queue-cap", description = "размер очередей между стадиями")
        .default(4)
    val innerName by parser
        .option(
            ArgType.Choice(listOf("none", "per-pixel", "by-rows", "by-columns", "grid"), { it }),
            fullName = "inner",
            description = "параллельность внутри одной свёртки",
        ).default("none")
    parser.parse(args)

    val kernel = Kernels.byName.getValue(kernelName)
    val workers = if (convWorkers == 0) Runtime.getRuntime().availableProcessors() else convWorkers
    val innerStrategy =
        when (innerName) {
            "none" -> null
            "per-pixel" -> ParallelStrategy.PER_PIXEL
            "by-rows" -> ParallelStrategy.BY_ROWS
            "by-columns" -> ParallelStrategy.BY_COLUMNS
            "grid" -> ParallelStrategy.GRID
            else -> error("Неизвестная стратегия: $innerName")
        }

    val inDir = Path.of(inputDir)
    val outDir = Path.of(outputDir)
    val imageExtensions = setOf("png", "jpg", "jpeg")
    val inputPaths =
        Files.list(inDir).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.extension.lowercase() in imageExtensions }
                .sorted()
                .toList()
        }
    require(inputPaths.isNotEmpty()) { "В папке $inDir нет картинок (.png/.jpg/.jpeg)" }
    Files.createDirectories(outDir)

    println("вход:        $inDir (${inputPaths.size} картинок)")
    println("выход:       $outDir")
    println("ядро:        $kernelName")
    println("conv-воркеры: $workers")
    println("queue-cap:   $queueCap")
    println("inner:       $innerName")

    val images = inputPaths.map { ImageIO.load(it) }
    lateinit var results: List<workshop.parallels.core.Image>
    val ms =
        measureTimeMillis {
            results = pipeline(images, kernel, convWorkers = workers, queueCap = queueCap, innerStrategy = innerStrategy)
        }

    for ((i, path) in inputPaths.withIndex()) {
        val outName = path.name.substringBeforeLast('.') + ".png"
        ImageIO.save(results[i], outDir.resolve(outName))
    }

    println("pipeline:    $ms мс (${"%.1f".format(inputPaths.size * 1000.0 / ms)} img/s)")
}
