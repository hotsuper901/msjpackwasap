pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // ffmpeg-kit binaries were pulled from Maven Central when the project
        // shut down; the Aliyun public mirror retains the cached JCenter builds
        // (ffmpeg-kit-full:6.0.LTS and its dependencies).
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
}

rootProject.name = "MSJStickerStudio"
include(":app")
