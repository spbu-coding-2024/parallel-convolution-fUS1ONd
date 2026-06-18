// task4 — GPU-свёртка одной картинки через OpenCL (JOCL).
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.cli)
}

application {
    mainClass = "workshop.parallels.task4.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
