package workshop.parallels.task1

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import workshop.parallels.core.ImageIO
import workshop.parallels.core.Kernels
import workshop.parallels.core.convolve
import java.nio.file.Path
import kotlin.system.measureTimeMillis

// CLI задачи 1: загрузить картинку, применить выбранное ядро, сохранить и напечатать тайминг.
fun main(args: Array<String>) {
    val parser = ArgParser("task1")
    val input by parser
        .option(ArgType.String, shortName = "i", description = "путь к входной картинке")
        .default("samples/img1.jpg")
    val output by parser
        .option(ArgType.String, shortName = "o", description = "путь к выходной картинке")
        .default("out/task1.png")
    val kernelName by parser
        .option(
            ArgType.Choice(Kernels.byName.keys.toList(), { it }),
            shortName = "k",
            description = "имя ядра",
        ).default("gaussian")
    parser.parse(args)

    val kernel = Kernels.byName.getValue(kernelName)
    val inputPath = Path.of(input)
    val outputPath = Path.of(output)

    println("вход:   $inputPath")
    println("выход:  $outputPath")
    println("ядро:   $kernelName")

    val image = ImageIO.load(inputPath)
    println("размер: ${image.width}x${image.height}")

    lateinit var result: workshop.parallels.core.Image
    val ms = measureTimeMillis { result = convolve(image, kernel) }
    ImageIO.save(result, outputPath)

    println("свёртка: $ms мс")
}
