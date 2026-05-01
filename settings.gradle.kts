pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rutapp"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:common")
include(":feature:auth")
include(":feature:home")
include(":feature:rutas")
include(":feature:mapa")
include(":feature:visita")
include(":feature:kpis")
include(":feature:calendario")
include(":feature:importar")
include(":feature:admin")
include(":feature:perfil")
