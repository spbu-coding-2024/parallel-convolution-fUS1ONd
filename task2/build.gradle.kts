// task2 — параллельная реализация свёртки.
// CLI-приложение, стратегия и число потоков задаются через аргументы.
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.cli)
}

application {
    mainClass = "workshop.parallels.task2.MainKt"
}

// Запускать из корня проекта, чтобы относительные пути типа samples/img1.jpg работали.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
