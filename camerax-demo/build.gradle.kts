plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.nav.safeargs) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
