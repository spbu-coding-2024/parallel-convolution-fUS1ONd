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
    resultFormat = "JSON"
    resultsFile = project.file("${project.layout.buildDirectory.get()}/results/jmh/results.json")
    fork = 1
    warmupIterations = 3
    iterations = 5
    // Абсолютный путь к samples/ передаётся в форкнутый JVM явно,
    // т.к. рабочая директория форка не совпадает с корнем проекта.
    jvmArgs = listOf("-Dbenchmarks.samplesDir=${rootProject.projectDir}/samples")
}
