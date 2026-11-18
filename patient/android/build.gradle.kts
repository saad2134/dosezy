// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}

// The dependencies block has been removed.

subprojects {
    tasks.withType<Test>().configureEach {
        val logsDir = rootProject.layout.projectDirectory.dir("logs").asFile
        jvmArgs(
            "-XX:ErrorFile=${logsDir.absolutePath}/hs_err_pid%p.log",
            "-XX:ReplayDataFile=${logsDir.absolutePath}/replay_pid%p.log"
        )
    }

    plugins.withId("com.android.application") {
        tasks.named("clean").configure {
            dependsOn(rootProject.tasks.named("pruneCrashLogs"))
        }
    }
}

tasks.register("pruneCrashLogs") {
    description = "Prunes old JVM crash logs keeping only the most recent 3 for debugging."
    group = "verification"
    doLast {
        val logsDir = rootProject.layout.projectDirectory.dir("logs").asFile
        if (logsDir.exists()) {
            val crashLogs = logsDir.listFiles { file ->
                file.name.endsWith(".log") && (file.name.startsWith("hs_err_pid") || file.name.startsWith("replay_pid"))
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val maxLogsToKeep = 3
            if (crashLogs.size > maxLogsToKeep) {
                crashLogs.drop(maxLogsToKeep).forEach { it.delete() }
            }

            if (crashLogs.isNotEmpty()) {
                val latest = crashLogs.first()
                logger.lifecycle("[JVM Diagnostics] Active crash dump available for debugging: logs/${latest.name}")
            }
        }
    }
}

tasks.register<Delete>("cleanCrashLogs") {
    description = "Purges all JVM crash and replay logs from the workspace."
    group = "cleanup"
    delete(fileTree(project.rootDir) {
        include("logs/*pid*.log")
        include("**/*pid*.log")
    })
}

tasks.register<Delete>("clean") {
    description = "Deletes build output directories and prunes old crash dumps."
    group = "build"
    delete(rootProject.layout.buildDirectory)
    dependsOn("pruneCrashLogs")
}
