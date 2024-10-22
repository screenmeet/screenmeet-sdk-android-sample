buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
    }
}

allprojects {
    repositories {
        mavenLocal()
        val mavenUrl = System.getenv("SM_MAVEN_REPO")
        val user = System.getenv("SM_SDK_USER")
        val pass = System.getenv("SM_SDK_PASS")

        val hasMaven = !mavenUrl.isNullOrBlank()
        val hasCredentials = !user.isNullOrBlank() && !pass.isNullOrBlank()

        if (hasMaven) {
            if (hasCredentials) {
                maven {
                    url = uri(mavenUrl)
                    credentials {
                        username = user
                        password = pass
                    }
                }
            } else {
                maven {
                    url = uri(mavenUrl)
                }
            }
        }

        maven {
            url = uri("https://nexus.screenmeet.com/repository/maven-releases/")
        }

        maven {
            // All of React Native (JS, Android binaries) is installed from npm
            url = uri("$rootDir/../node_modules/react-native/android")
        }
        maven {
            // Android JSC is installed from npm
            url = uri("$rootDir/../node_modules/jsc-android/dist")
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
