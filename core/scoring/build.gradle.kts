import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    id("perigee.kmp.serialization")
}

abstract class GenerateWeightsSource : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val weightsJson: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val text = weightsJson.get().asFile.readText().replace("$", "\${'$'}")
        val packageDirectory = outputDirectory.get().asFile.resolve("brenodega28/perigee/core/scoring")
        packageDirectory.deleteRecursively()
        packageDirectory.mkdirs()
        packageDirectory.resolve("EmbeddedWeights.kt").writeText(
            "package brenodega28.perigee.core.scoring\n\n" +
                "internal object EmbeddedWeights {\n" +
                "    const val JSON: String = \"\"\"$text\"\"\"\n" +
                "}\n",
        )
    }
}

val generateWeightsSource =
    tasks.register<GenerateWeightsSource>("generateWeightsSource") {
        weightsJson = layout.projectDirectory.file("weights.json")
        outputDirectory = layout.buildDirectory.dir("generated/weights/commonMain/kotlin")
    }

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateWeightsSource.flatMap { it.outputDirectory })
            dependencies {
                api(project(":core:model"))
            }
        }
    }
}
