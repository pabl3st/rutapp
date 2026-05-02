plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library)     apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt)                apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.google.services)     apply false
    alias(libs.plugins.crashalytics.plugin) apply false
}

// ── Git hooks — configurar automáticamente al hacer sync ────
tasks.register("installGitHooks") {
    description = "Instala los hooks de git desde .githooks/"
    group       = "setup"
    doLast {
        val hooksDir = rootProject.file(".githooks")
        val gitHooksDir = rootProject.file(".git/hooks")
        if (hooksDir.exists() && gitHooksDir.exists()) {
            hooksDir.listFiles()
                ?.filter { it.name != "install.sh" }
                ?.forEach { hook ->
                    val target = File(gitHooksDir, hook.name)
                    hook.copyTo(target, overwrite = true)
                    target.setExecutable(true)
                    println("✅ Hook instalado: ${hook.name}")
                }
        }
    }
}

// Auto-instalar hooks al preparar el proyecto
tasks.named("prepareKotlinBuildScriptModel") {
    dependsOn("installGitHooks")
}
