import io.github.hfhbd.mavencentral.*

plugins {
    id("maven-publish")
    id("signing")
}

val mavenCentralWorker = configurations.dependencyScope("mavenCentralWorker")
val mavenCentralWorkerClassPath = configurations.resolvable("mavenCentralWorkerClasspath") {
    extendsFrom(mavenCentralWorker.get())
}

dependencies {
    mavenCentralWorker("io.github.hfhbd.maven-central:runtime:$VERSION")
}

val publishToMavenCentral by tasks.registering {
    group = "publishing"
}

tasks.publish {
    dependsOn(publishToMavenCentral)
}

publishing {
    val localMavenCentralRepoDir = layout.buildDirectory.dir("mavencentral/repo")
    val repoName = "localMavenCentral"


    repositories.maven {
        name = repoName
        url = uri(localMavenCentralRepoDir)
    }

    publications.withType<MavenPublication>().all {
        val pubName = name.replaceFirstChar { it.uppercaseChar() }

        val publishToLocalMavenCentral =
            tasks.named(
                "publish${pubName}PublicationTo${repoName.replaceFirstChar { it.uppercaseChar() }}Repository",
                PublishToMavenRepository::class.java
            ) {
                doFirst {
                    localMavenCentralRepoDir.get().asFile.deleteRecursively()
                }
            }

        val createMavenCentralZipFile = tasks.register("createMavenCentralZipFile${pubName}", Zip::class) {
            dependsOn(publishToLocalMavenCentral)
            archiveFileName.set("${project.group}-${project.name}-${version}.zip")
            from(localMavenCentralRepoDir) {
                exclude {
                    it.name.startsWith("maven-metadata.xml")
                }
            }
            destinationDirectory.set(layout.buildDirectory.dir("mavencentral/publishing"))
        }

        val publishToMavenCentralTask = tasks.register(
            "publish${pubName}PublicationToMavenCentral",
            PublishToMavenCentral::class,
        ) {
            group = "publishing"
            uploadZip.set(createMavenCentralZipFile.flatMap {
                it.archiveFile
            })
            workerClassPath.from(mavenCentralWorkerClassPath)
        }

        publishToMavenCentral {
            dependsOn(publishToMavenCentralTask)
        }
    }
}
