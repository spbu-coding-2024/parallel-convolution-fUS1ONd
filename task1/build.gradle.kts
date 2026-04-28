// task1 — последовательная реализация свёртки.
// CLI-приложение, парсинг аргументов и дефолты — внутри Main.kt.
plugins {
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass = "workshop.parallels.task1.MainKt"
}
