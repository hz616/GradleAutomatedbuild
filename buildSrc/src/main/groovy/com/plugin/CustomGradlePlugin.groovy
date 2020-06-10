package com.plugin


import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomGradlePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        project.extensions.create("releaseInfo", ReleaseInfoExtension.class)


        project.tasks.create("releaseInfoTask", ReleaseInfoTask.class)

        println "hello plugin " + project.name
    }
}