package workshop.parallels.benchmarks

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import workshop.parallels.core.Image
import workshop.parallels.core.ImageIO
import workshop.parallels.core.Kernels
import workshop.parallels.core.ParallelStrategy
import workshop.parallels.core.convolve
import workshop.parallels.core.convolveParallel
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
open class ParallelConvolutionBench {
    // --- Бенчмарк 1: сравнение стратегий + последовательной ---
    // img4 (1024x1024), gaussian 3x3, N = availableProcessors потоков.

    @State(Scope.Benchmark)
    open class StrategyState {
        @Param("SEQUENTIAL", "PER_PIXEL", "BY_ROWS", "BY_COLUMNS", "GRID")
        lateinit var strategyName: String

        lateinit var image: Image

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/img4.jpg"))
        }
    }

    @Benchmark
    fun benchStrategy(state: StrategyState): Image {
        val threads = Runtime.getRuntime().availableProcessors()
        return if (state.strategyName == "SEQUENTIAL") {
            convolve(state.image, Kernels.GAUSSIAN)
        } else {
            convolveParallel(state.image, Kernels.GAUSSIAN, ParallelStrategy.valueOf(state.strategyName), threads)
        }
    }

    // --- Бенчмарк 2: масштабируемость по числу потоков (BY_ROWS) ---
    // img4 (1024x1024), gaussian 3x3, потоки варьируются.
    // Внимание: создание и завершение ExecutorService входит в каждое измерение,
    // поэтому результаты отражают полную стоимость вызова convolveParallel,
    // а не только вычислительное ядро свёртки.

    @State(Scope.Benchmark)
    open class ThreadCountState {
        @Param("1", "2", "4", "8", "16")
        var threads: Int = 1

        lateinit var image: Image

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/img4.jpg"))
        }
    }

    @Benchmark
    fun benchThreadCount(state: ThreadCountState): Image =
        convolveParallel(state.image, Kernels.GAUSSIAN, ParallelStrategy.BY_ROWS, state.threads)

    // --- Бенчмарк 3: время vs размер изображения (BY_ROWS, N потоков) ---
    // Аналог benchImageSize из ConvolutionBench для сравнения parallel vs sequential.

    @State(Scope.Benchmark)
    open class ParallelImageState {
        @Param("img1", "img2", "img3", "img4", "img5")
        lateinit var imageName: String

        lateinit var image: Image

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/$imageName.jpg"))
        }
    }

    @Benchmark
    fun benchParallelImageSize(state: ParallelImageState): Image = convolveParallel(
        state.image,
        Kernels.GAUSSIAN,
        ParallelStrategy.BY_ROWS,
        Runtime.getRuntime().availableProcessors(),
    )
}
