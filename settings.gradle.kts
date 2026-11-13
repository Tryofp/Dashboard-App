pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "DashboardApp"
include(":app", ":core:network", ":core:security", ":core:notifications", ":feature:pluginmanager", ":feature:tracker", ":feature:usage", ":feature:donghua")
