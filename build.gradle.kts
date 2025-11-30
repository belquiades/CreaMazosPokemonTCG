// build.gradle.kts (root) - minimal, repositories are managed in settings.gradle.kts
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

