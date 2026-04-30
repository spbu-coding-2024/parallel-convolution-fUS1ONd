// Модуль JMH-бенчмарков. Источники в src/jmh/kotlin/ — source set создаётся плагином автоматически.
plugins {
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(project(":core"))
    // Аннотационный процессор нужен явно: плагин не добавляет его для Kotlin автоматически.
    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
}

jmh {
    // Фильтрация бенчмарков по имени класса через -Pjmh.include=<regex>.
    // Пример: ./gradlew :benchmarks:jmh -Pjmh.include=ParallelConvolutionBench
    val includeFilter = project.findProperty("jmh.include") as String?
    if (includeFilter != null) {
        includes = listOf(includeFilter)
    }
    resultFormat = "JSON"
    // Имя файла результатов параметризуется через -Pjmh.rff=<name>.json,
    // чтобы Makefile мог писать TASK=1 и TASK=2 в разные файлы и переиспользовать их.
    val rff = project.findProperty("jmh.rff") as String? ?: "results.json"
    resultsFile = project.file("${project.layout.buildDirectory.get()}/results/jmh/$rff")
    fork = 1
    warmupIterations = 3
    iterations = 5
    // Абсолютный путь к samples/ передаётся в форкнутый JVM явно,
    // т.к. рабочая директория форка не совпадает с корнем проекта.
    jvmArgs = listOf("-Dbenchmarks.samplesDir=${rootProject.projectDir}/samples")
}
