// task3 — pipeline-обработка массива изображений (reader/conv/writer).
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.cli)
}

application {
    mainClass = "workshop.parallels.task3.MainKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
