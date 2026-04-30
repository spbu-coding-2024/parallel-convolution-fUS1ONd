// task1 — последовательная реализация свёртки.
// CLI-приложение, парсинг аргументов и дефолты — внутри Main.kt.
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.cli)
}

application {
    mainClass = "workshop.parallels.task1.MainKt"
}

// Запускать из корня проекта, чтобы относительные пути типа samples/img1.jpg работали.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
