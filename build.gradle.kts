plugins {
    // Плагины применяются только к подпроектам (через subprojects ниже),
    // в корне не активируем — указываем `apply false`.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.jmh) apply false
}

// Репозитории для корня нужны Spotless: он скачивает ktlint в configuration
// корневого проекта. Подпроекты получают свои repositories через subprojects {} ниже.
repositories {
    mavenCentral()
}

// Общие настройки для всех модулей проекта.
// Чтобы каждый модуль не дублировал repositories, java toolchain, тестовый фреймворк и т.п.
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    // Единая версия JDK для всех модулей.
    // Используем системный JDK; авто-скачивание JDK через foojay отключено в gradle.properties.
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        "testImplementation"(libs.findLibrary("junit-jupiter").get())
        "testImplementation"(libs.findLibrary("kotest-runner-junit5").get())
        "testImplementation"(libs.findLibrary("kotest-property").get())
        "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}
