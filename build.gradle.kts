// Root project has no sources — it exists solely to aggregate subprojects.
// All build configuration is in convention plugins inside buildSrc/.

tasks.register("printVersion") {
    doLast { println(version) }
}

// Aggregate JaCoCo reports across all submodules
tasks.register<JacocoReport>("jacocoRootReport") {
    group = "verification"
    description = "Aggregates JaCoCo coverage reports from all subprojects"

    subprojects.forEach { subproject ->
        subproject.plugins.withType<JacocoPlugin> {
            val testTask = subproject.tasks.findByName("test") as? Test ?: return@withType
            val reportTask = subproject.tasks.findByName("jacocoTestReport") as? JacocoReport ?: return@withType
            dependsOn(testTask, reportTask)
            executionData(testTask)
            sourceSets(subproject.the<SourceSetContainer>()["main"])
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
