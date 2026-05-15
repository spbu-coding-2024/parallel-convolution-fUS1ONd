// core — общая библиотека: Image, Kernel, свёртка, IO, тесты-свойства.
// Используется всеми модулями task1, task2, task3, task4.
// java-library плагин даёт конфигурацию `api` для проброса транзитивных зависимостей.
plugins {
    `java-library`
}

dependencies {
    // JOCL — Java-биндинги OpenCL для GPU-свёртки (task4).
    api(libs.jocl)
}
