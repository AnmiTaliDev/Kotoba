import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

abstract class BuildEngineLibraries : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val engineSources: ConfigurableFileCollection

    @get:Input
    abstract val ndkDirectory: Property<String>

    @get:Input
    abstract val jobs: Property<Int>

    @get:Internal
    abstract val engineDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val jniLibsDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun build() {
        val home = System.getProperty("user.home")
        val path = "$home/.cargo/bin" + File.pathSeparator + (System.getenv("PATH") ?: "")
        try {
            execOperations.exec {
                commandLine("cargo", "ndk", "--version")
                environment("PATH", path)
            }
        } catch (error: Exception) {
            throw GradleException(
                "The Rust toolchain is required to build the engine: install rustup, the Android targets " +
                    "(aarch64-linux-android, armv7-linux-androideabi, x86_64-linux-android) and cargo-ndk. " +
                    "See the Build section of README.md.",
                error,
            )
        }
        execOperations.exec {
            workingDir = engineDirectory.get().asFile
            environment("PATH", path)
            environment("ANDROID_NDK_HOME", ndkDirectory.get())
            commandLine(
                "cargo", "ndk",
                "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64",
                "-P", "31",
                "-o", jniLibsDirectory.get().asFile.absolutePath,
                "build", "-p", "kotoba-ffi", "--release", "--jobs", jobs.get().toString(),
            )
        }
    }
}

abstract class GenerateEngineBindings : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val engineSources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val library: RegularFileProperty

    @get:Input
    abstract val jobs: Property<Int>

    @get:Internal
    abstract val engineDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val home = System.getProperty("user.home")
        val path = "$home/.cargo/bin" + File.pathSeparator + (System.getenv("PATH") ?: "")
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        execOperations.exec {
            workingDir = engineDirectory.get().asFile
            environment("PATH", path)
            commandLine(
                "cargo", "run", "--quiet", "--jobs", jobs.get().toString(), "-p", "kotoba-uniffi-bindgen", "--",
                "generate", "--no-format", "--library", library.get().asFile.absolutePath,
                "--language", "kotlin", "--out-dir", output.absolutePath,
            )
        }
    }
}

val engineRoot = rootProject.layout.projectDirectory.dir("engine")
val engineSourceTree = fileTree(engineRoot) {
    include("Cargo.toml", "Cargo.lock", "crates/**", "data/**")
    exclude("target/**")
}
val cargoJobs = providers.gradleProperty("kotoba.cargo.jobs").map(String::toInt).orElse(2)

android {
    namespace = "dev.anmitali.kotoba"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "dev.anmitali.kotoba"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

val buildEngineLibraries = tasks.register<BuildEngineLibraries>("buildEngineLibraries") {
    group = "engine"
    description = "Builds the Rust engine for every Android ABI."
    this.engineSources.from(engineSourceTree)
    engineDirectory.set(engineRoot)
    ndkDirectory.set(androidComponents.sdkComponents.ndkDirectory.map { it.asFile.absolutePath })
    jobs.set(cargoJobs)
    jniLibsDirectory.set(layout.buildDirectory.dir("rustJniLibs"))
}

val generateEngineBindings = tasks.register<GenerateEngineBindings>("generateEngineBindings") {
    group = "engine"
    description = "Generates the Kotlin bindings of the Rust engine."
    this.engineSources.from(engineSourceTree)
    engineDirectory.set(engineRoot)
    library.set(buildEngineLibraries.flatMap { it.jniLibsDirectory.file("arm64-v8a/libkotoba_ffi.so") })
    jobs.set(cargoJobs)
    outputDirectory.set(layout.buildDirectory.dir("generated/engine-bindings"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(buildEngineLibraries, BuildEngineLibraries::jniLibsDirectory)
        variant.sources.kotlin?.addGeneratedSourceDirectory(generateEngineBindings, GenerateEngineBindings::outputDirectory)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(variantOf(libs.jna) { artifactType("aar") })

    testImplementation(libs.junit)
}
