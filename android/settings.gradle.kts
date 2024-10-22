import groovy.lang.Closure
import java.io.FileInputStream
import java.util.Properties

rootProject.name = "ScreenMeet Live"
include(":app")

loadProps()

fun Settings.loadProps() {
    val gradlePropertiesFile = File(rootDir, "gradle.properties")
    val gradleProperties = Properties()

    if (gradlePropertiesFile.exists()) {
        FileInputStream(gradlePropertiesFile).use { fis ->
            gradleProperties.load(fis)
        }
    } else {
        println("gradle.properties file not found at ${gradlePropertiesFile.absolutePath}")
    }

    val includeFlutter: Boolean = gradleProperties.getProperty("includeFlutter")?.toBoolean() ?: false
    val includeReactNative: Boolean = gradleProperties.getProperty("includeReactNative")?.toBoolean() ?: false

    if (includeFlutter) {
        include(":flutter_demo", ":flutter_module")
        apply(from = "flutter_module/.android/include_flutter.gradle.kts")
    }

    if (includeReactNative) {
        include(":react_demo")
        apply(from = "../node_modules/@react-native-community/cli-platform-android/native_modules.gradle")
        val applyNativeModules = extra.get("applyNativeModulesSettingsGradle") as Closure<*>
        applyNativeModules(settings)
    }
}
