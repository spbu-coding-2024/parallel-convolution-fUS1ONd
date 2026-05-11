package workshop.parallels.core

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

// Pipeline-обработка массива изображений: reader -> conv -> writer с bounded-очередями.
fun pipeline(
    images: List<Image>,
    kernel: Kernel,
    convWorkers: Int = Runtime.getRuntime().availableProcessors(),
    queueCap: Int = 4,
    innerStrategy: ParallelStrategy? = null,
    border: BorderStrategy = BorderStrategy.CLAMP,
): List<Image> {
    if (images.isEmpty()) return emptyList()
    require(convWorkers >= 1) { "convWorkers must be >= 1" }
    require(queueCap >= 1) { "queueCap must be >= 1" }

    val workers = minOf(convWorkers, images.size)
    val inputQueue = ArrayBlockingQueue<InputItem>(queueCap)
    val outputQueue = ArrayBlockingQueue<IndexedImage>(queueCap)
    val results = arrayOfNulls<Image>(images.size)

    val readerPool = Executors.newSingleThreadExecutor()
    val convPool = Executors.newFixedThreadPool(workers)
    val writerPool = Executors.newSingleThreadExecutor()

    try {
        val reader =
            readerPool.submit {
                for ((i, img) in images.withIndex()) {
                    inputQueue.put(InputItem.Task(IndexedImage(i, img)))
                }
                // poison pills для каждого воркера
                repeat(workers) { inputQueue.put(InputItem.Poison) }
            }

        val convFutures =
            (0 until workers).map {
                convPool.submit {
                    while (true) {
                        val item = inputQueue.take()
                        if (item is InputItem.Poison) break
                        val task = (item as InputItem.Task).value
                        val out =
                            if (innerStrategy == null) {
                                convolve(task.image, kernel, border)
                            } else {
                                convolveParallel(task.image, kernel, innerStrategy, border = border)
                            }
                        outputQueue.put(IndexedImage(task.index, out))
                    }
                }
            }

        val writer =
            writerPool.submit {
                repeat(images.size) {
                    val item = outputQueue.take()
                    results[item.index] = item.image
                }
            }

        reader.get()
        convFutures.forEach { it.get() }
        writer.get()
    } finally {
        readerPool.shutdown()
        convPool.shutdown()
        writerPool.shutdown()
    }

    @Suppress("UNCHECKED_CAST")
    return results.toList() as List<Image>
}

private data class IndexedImage(
    val index: Int,
    val image: Image,
)

private sealed interface InputItem {
    data class Task(
        val value: IndexedImage,
    ) : InputItem

    data object Poison : InputItem
}
