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
import workshop.parallels.core.Kernel
import workshop.parallels.core.Kernels
import workshop.parallels.core.convolve
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
open class ConvolutionBench {
    // --- Бенчмарк 1: время свёртки vs размер изображения ---
    // Ядро фиксировано (gaussian 3×3), картинки варьируются.

    @State(Scope.Benchmark)
    open class ImageState {
        @Param("img1", "img2", "img3", "img4", "img5")
        lateinit var imageName: String

        lateinit var image: Image
        val kernel: Kernel = Kernels.GAUSSIAN

        // Картинка загружается один раз на Trial — IO не входит в измерение.
        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/$imageName.jpg"))
        }
    }

    @Benchmark
    fun benchImageSize(state: ImageState): Image = convolve(state.image, state.kernel)

    // --- Бенчмарк 2: время свёртки vs размер ядра ---
    // Картинка фиксирована (img3), ядра варьируются по размеру (3×3, 5×5, 9×9).

    @State(Scope.Benchmark)
    open class KernelState {
        @Param("box-blur", "gaussian", "gaussian-5x5", "motion-blur")
        lateinit var kernelName: String

        lateinit var kernel: Kernel
        lateinit var image: Image

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/img4.jpg"))
            kernel = Kernels.byName.getValue(kernelName)
        }
    }

    @Benchmark
    fun benchKernelSize(state: KernelState): Image = convolve(state.image, state.kernel)
}
