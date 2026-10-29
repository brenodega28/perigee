import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

const val ANDROID_COMPILE_SDK = 36
const val ANDROID_MIN_SDK = 26
const val ANDROID_TARGET_SDK = 36
const val JVM_TOOLCHAIN = 21
val JAVA_TARGET: JavaVersion = JavaVersion.VERSION_21

val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.perigeeNamespace(): String =
    "brenodega28.perigee." + path.removePrefix(":").replace(':', '.').replace('-', '.')
