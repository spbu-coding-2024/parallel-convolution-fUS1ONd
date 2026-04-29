plugins {
    // Плагин позволяет автоматически скачивать JDK нужной версии (используется toolchain в build.gradle.kts).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "workshop-parallels"

include("core")
include("task1")
include("benchmarks")
