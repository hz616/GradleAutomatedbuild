package com.hz.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class ReleaseInfoTask extends DefaultTask {


    ReleaseInfoTask() {
        group "version_manager"
        description "release info "
    }


    @TaskAction
    void doAction() {
        println("release info task exec")
        println("config versionName $project.extensions.releaseInfo.versionName")
        println("config versionCode $project.extensions.releaseInfo.versionCode")
    }


}
