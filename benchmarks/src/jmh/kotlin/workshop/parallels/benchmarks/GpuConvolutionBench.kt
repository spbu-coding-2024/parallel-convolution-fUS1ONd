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
import workshop.parallels.core.ParallelStrategy
import workshop.parallels.core.convolve
import workshop.parallels.core.convolveGpu
import workshop.parallels.core.convolveParallel
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
open class GpuConvolutionBench {
    // --- Бенчмарк 1: GPU vs CPU vs CPU-parallel по размеру картинки ---
    // Ядро фиксировано (gaussian 3×3), картинки img1..img5 (256..2048).
    // Показывает, на каком размере GPU начинает обгонять CPU.

    @State(Scope.Benchmark)
    open class ImageSizeState {
        @Param("img1", "img2", "img3", "img4", "img5")
        lateinit var imageName: String

        lateinit var image: Image
        val kernel: Kernel = Kernels.GAUSSIAN

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/$imageName.jpg"))
        }
    }

    @Benchmark
    fun benchGpuByImageSize(state: ImageSizeState): Image = convolveGpu(state.image, state.kernel)

    @Benchmark
    fun benchCpuByImageSize(state: ImageSizeState): Image = convolve(state.image, state.kernel)

    @Benchmark
    fun benchCpuParallelByImageSize(state: ImageSizeState): Image = convolveParallel(state.image, state.kernel, ParallelStrategy.BY_ROWS)

    // --- Бенчмарк 2: GPU vs CPU vs CPU-parallel по размеру ядра ---
    // Картинка фиксирована (img4, 1024×1024), ядра 3×3, 5×5, 9×9.
    // Свёртка квадратична по размеру ядра — GPU должен выигрывать тем сильнее, чем больше ядро.

    @State(Scope.Benchmark)
    open class KernelSizeState {
        @Param("gaussian", "gaussian-5x5", "motion-blur")
        lateinit var kernelName: String

        lateinit var image: Image
        lateinit var kernel: Kernel

        @Setup(Level.Trial)
        fun setup() {
            val samplesDir = System.getProperty("benchmarks.samplesDir", "samples")
            image = ImageIO.load(Paths.get("$samplesDir/img4.jpg"))
            kernel = Kernels.byName.getValue(kernelName)
        }
    }

    @Benchmark
    fun benchGpuByKernelSize(state: KernelSizeState): Image = convolveGpu(state.image, state.kernel)

    @Benchmark
    fun benchCpuByKernelSize(state: KernelSizeState): Image = convolve(state.image, state.kernel)

    @Benchmark
    fun benchCpuParallelByKernelSize(state: KernelSizeState): Image = convolveParallel(state.image, state.kernel, ParallelStrategy.BY_ROWS)
}
