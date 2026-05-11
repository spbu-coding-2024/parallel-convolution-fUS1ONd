package workshop.parallels.core

// Pipeline-обработка массива изображений: reader -> conv -> writer с bounded-очередями.
fun pipeline(
    images: List<Image>,
    kernel: Kernel,
    convWorkers: Int = Runtime.getRuntime().availableProcessors(),
    queueCap: Int = 4,
    innerStrategy: ParallelStrategy? = null,
    border: BorderStrategy = BorderStrategy.CLAMP,
): List<Image> = TODO("шаг 2: реализация Pipeline")
