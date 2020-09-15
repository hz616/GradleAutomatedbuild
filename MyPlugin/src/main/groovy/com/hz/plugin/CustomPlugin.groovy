//这里一定要注意包名，没有包名找不到类
package com.hz.plugin

import com.android.build.gradle.AppExtension
import com.hz.plugin.extension.AutoTrackConfig
import com.hz.plugin.transform.MyTransform
import com.hz.plugin.util.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "MyPlugin and success"

//        project.extensions.create("releaseInfo", ReleaseInfoExtension)

//        def releaseInfoTask = project.tasks.create("releaseInfoTask", ReleaseInfoTask)
//        //使preBuild依赖我们这个自定的task，就可以在编译阶段自动执行
//        project.tasks.findByName("preBuild").dependsOn(releaseInfoTask)

        project.extensions.create('autoTrackConfig', AutoTrackConfig)
        def appExt = project.extensions.findByType(AppExtension.class)
        appExt.registerTransform(new MyTransform())

        project.afterEvaluate {
            Logger.setDebug(project.extensions.autoTrackConfig.isDebug)
            Logger.info("autoTrackConfig hookMethod is $project.extensions.autoTrackConfig.hookMethod")
            GlobalConfig.instance.setAutoTrackBase(project.extensions.autoTrackConfig.hookMethod)
            GlobalConfig.instance.setIsOpenAutoTrack(project.extensions.autoTrackConfig.isOpenAutoTrack)
        }

    }
}