package com.plugin

import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class ReleaseInfoTask extends DefaultTask {


    ReleaseInfoTask() {
        group = 'version_manager'
        description = 'release info update'
    }

    @TaskAction
    void doAction() {
        updateVersionInfo()
    }


    private void updateVersionInfo() {
        def versionCodeMsg = project.extensions.releaseInfo.versionCode
        def versionNameMsg = project.extensions.releaseInfo.versionName
        def versionInfoMsg = project.extensions.releaseInfo.versionInfo
        def fileName = project.extensions.releaseInfo.fileName
        def file = project.file(fileName)
        def sw = new StringWriter()
        def xmlBuilder = new MarkupBuilder(sw)
        if (file.text != null && file.text.size() <= 0) {
            xmlBuilder.releases {
                release {
                    versionCode(versionCodeMsg)
                    versionName(versionNameMsg)
                    versionInfo(versionInfoMsg)
                }
            }
            file.withWriter { writer -> writer.append(sw.toString()) }
        } else {
            xmlBuilder.release {
                versionCode(versionCodeMsg)
                versionName(versionNameMsg)
                versionInfo(versionInfoMsg)
            }
            file.withWriter { writer ->
                lines.eachWithIndex { line, index ->
                    if (index != lengths) {
                        writer.append(line + '\r\n')
                    }else if(index == lengths){
                        writer.append('\r\r\n' + sw.toString() + '\r\n')
                        writer.append(lines.get(tlengths))
                    }
                }
            }

        }
    }


}