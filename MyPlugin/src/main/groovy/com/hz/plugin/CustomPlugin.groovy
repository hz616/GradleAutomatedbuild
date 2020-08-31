//这里一定要注意包名，没有包名找不到类
package com.hz.plugin


import com.hz.plugin.transform.MyTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension

class CustomPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "MyPlugin and success"

        project.task("pluginTask") {
            group "pluginTask"
            description "this is a custom plugin task and create by project"
            doLast {
                println("run plugin task")
            }

        }

//        project.extensions.create("releaseInfo", ReleaseInfoExtension)

//        def releaseInfoTask = project.tasks.create("releaseInfoTask", ReleaseInfoTask)
//        //使preBuild依赖我们这个自定的task，就可以在编译阶段自动执行
//        project.tasks.findByName("preBuild").dependsOn(releaseInfoTask)


        def appExt = project.extensions.findByType(AppExtension.class)
        appExt.registerTransform(new MyTransform())

    }
}