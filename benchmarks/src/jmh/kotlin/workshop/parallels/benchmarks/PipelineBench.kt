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
import workshop.parallels.core.pipeline
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
open class PipelineBench {
    private companion object {
        // Размер пачки подобран так, чтобы workers <= BATCH_MEDIUM и хватало работы при cap=16.
        const val BATCH_MEDIUM = 32
        const val BATCH_LARGE = 8

        fun loadSample(name: String): Image {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            return ImageIO.load(Paths.get("$samplesDir/$name.jpg"))
        }
    }

    // --- Бенчмарк 1: масштабируемость по числу conv-воркеров ---
    // 32 копии img4 (1024x1024), gaussian 3x3, cap=4, inner=null.
    // workers=1 ≈ последовательная обработка списка (baseline ≈ task1 на массиве).

    @State(Scope.Benchmark)
    open class WorkersState {
        @Param("1", "2", "4", "8")
        var workers: Int = 1

        lateinit var images: List<Image>

        @Setup(Level.Trial)
        fun setup() {
            images = List(BATCH_MEDIUM) { loadSample("img4") }
        }
    }

    @Benchmark
    fun benchByConvWorkers(state: WorkersState): List<Image> =
        pipeline(state.images, Kernels.GAUSSIAN, convWorkers = state.workers, queueCap = 4)

    // --- Бенчмарк 2: влияние размера очереди ---
    // 32 копии img4, workers=4, inner=null. cap=1 сериализует стадии, далее — плато.

    @State(Scope.Benchmark)
    open class QueueCapState {
        @Param("1", "2", "4", "8", "16")
        var cap: Int = 1

        lateinit var images: List<Image>

        @Setup(Level.Trial)
        fun setup() {
            images = List(BATCH_MEDIUM) { loadSample("img4") }
        }
    }

    @Benchmark
    fun benchByQueueCap(state: QueueCapState): List<Image> = pipeline(state.images, Kernels.GAUSSIAN, convWorkers = 4, queueCap = state.cap)

    // --- Бенчмарк 3: внутренний параллелизм свёртки ---
    // 8 больших картинок (img5, 2048x2048), workers=2 — недозагружены ядра при inner=null.
    // inner=BY_ROWS должен дозагрузить простаивающие ядра внутри каждой свёртки.

    @State(Scope.Benchmark)
    open class InnerState {
        @Param("NONE", "BY_ROWS")
        lateinit var inner: String

        lateinit var images: List<Image>

        @Setup(Level.Trial)
        fun setup() {
            images = List(BATCH_LARGE) { loadSample("img5") }
        }
    }

    @Benchmark
    fun benchInnerStrategy(state: InnerState): List<Image> {
        val strategy = if (state.inner == "NONE") null else ParallelStrategy.valueOf(state.inner)
        return pipeline(state.images, Kernels.GAUSSIAN, convWorkers = 2, queueCap = 4, innerStrategy = strategy)
    }
}
